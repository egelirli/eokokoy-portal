package com.ekokoy.portal.dues.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.dues.dto.ImportDetailResponse;
import com.ekokoy.portal.dues.dto.ImportResponse;
import com.ekokoy.portal.dues.entity.Due;
import com.ekokoy.portal.dues.entity.DueImport;
import com.ekokoy.portal.dues.entity.ImportStatus;
import com.ekokoy.portal.dues.repository.DueImportRepository;
import com.ekokoy.portal.dues.repository.DueRepository;
import com.ekokoy.portal.user.entity.Property;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.repository.PropertyRepository;
import com.ekokoy.portal.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class DueImportService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DueImportRepository importRepository;
    private final DueRepository dueRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public DueImportService(DueImportRepository importRepository,
                            DueRepository dueRepository,
                            PropertyRepository propertyRepository,
                            UserRepository userRepository) {
        this.importRepository = importRepository;
        this.dueRepository = dueRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    /** CSV veya Excel dosyasını parse eder ve borçları upsert ile kaydeder. */
    @Transactional
    public ImportDetailResponse importFile(MultipartFile file) {
        validateFile(file);

        UUID userId = currentUserId();
        User importer = userRepository.getReferenceById(userId);

        DueImport batch = new DueImport();
        batch.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
        batch.setImportedBy(importer);
        batch.setStatus(ImportStatus.processing);
        importRepository.save(batch);

        List<String[]> rows;
        try {
            String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            if (name.endsWith(".xlsx")) {
                rows = parseExcel(file);
            } else {
                rows = parseCsv(file);
            }
        } catch (IOException e) {
            batch.setStatus(ImportStatus.failed);
            batch.setCompletedAt(Instant.now());
            importRepository.save(batch);
            throw new EkokoyException("IMPORT_PARSE_ERROR", "Dosya okunamadı: " + e.getMessage(), 422);
        }

        int total = rows.size();
        int success = 0;
        int errors = 0;
        List<Map<String, Object>> errorDetails = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNum = i + 2; // header satırı 1, veri 2'den başlar
            try {
                processRow(row, batch, importer);
                success++;
            } catch (Exception e) {
                errors++;
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("row", rowNum);
                detail.put("error", e.getMessage());
                if (row.length > 0) detail.put("konut_no", row[0]);
                errorDetails.add(detail);
            }
        }

        batch.setTotalRows(total);
        batch.setSuccessRows(success);
        batch.setErrorRows(errors);
        batch.setStatus(ImportStatus.completed);
        batch.setCompletedAt(Instant.now());
        if (!errorDetails.isEmpty()) {
            batch.setErrorDetails(serializeErrors(errorDetails));
        }
        importRepository.save(batch);

        return ImportDetailResponse.from(batch);
    }

    /** Tüm import kayıtlarını listeler. */
    @Transactional(readOnly = true)
    public List<ImportResponse> listImports() {
        return importRepository.findAll().stream()
                .sorted(Comparator.comparing(DueImport::getCreatedAt).reversed())
                .map(ImportResponse::from).toList();
    }

    /** Tek import kaydının detayını döner. */
    @Transactional(readOnly = true)
    public ImportDetailResponse getImport(UUID importId) {
        DueImport batch = importRepository.findById(importId)
                .orElseThrow(() -> new EkokoyException("IMPORT_NOT_FOUND", "Import kaydı bulunamadı.", 404));
        return ImportDetailResponse.from(batch);
    }

    // ── Parse edici yardımcılar ───────────────────────────────────────────────────

    private List<String[]> parseCsv(MultipartFile file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // header atla
                line = line.trim();
                if (line.isEmpty()) continue;
                rows.add(line.split(",", -1));
            }
        }
        return rows;
    }

    private List<String[]> parseExcel(MultipartFile file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; } // header atla
                // 6 sütun: konut_no, yil, ay, tutar, son_odeme_tarihi, aciklama
                String[] cells = new String[6];
                for (int c = 0; c < 6; c++) {
                    Cell cell = row.getCell(c);
                    cells[c] = cellToString(cell);
                }
                // Tamamen boş satırları atla
                boolean allBlank = Arrays.stream(cells).allMatch(s -> s == null || s.isBlank());
                if (!allBlank) rows.add(cells);
            }
        }
        return rows;
    }

    private String cellToString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) yield String.valueOf((long) val);
                yield String.valueOf(val);
            }
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { yield cell.getStringCellValue(); }
            }
            default -> "";
        };
    }

    // ── Satır işleme ─────────────────────────────────────────────────────────────

    private void processRow(String[] row, DueImport batch, User importer) {
        if (row.length < 5) {
            throw new IllegalArgumentException("Yetersiz sütun sayısı (en az 5 gerekli).");
        }

        String konutNoStr = trim(row[0]);
        String yilStr = trim(row[1]);
        String ayStr = row.length > 2 ? trim(row[2]) : "";
        String tutarStr = trim(row[3]);
        String sonOdemeTarihiStr = trim(row[4]);
        String aciklama = row.length > 5 ? trim(row[5]) : null;

        if (konutNoStr.isEmpty()) throw new IllegalArgumentException("konut_no boş olamaz.");
        if (yilStr.isEmpty()) throw new IllegalArgumentException("yil boş olamaz.");
        if (tutarStr.isEmpty()) throw new IllegalArgumentException("tutar boş olamaz.");
        if (sonOdemeTarihiStr.isEmpty()) throw new IllegalArgumentException("son_odeme_tarihi boş olamaz.");

        int konutNo;
        try { konutNo = Integer.parseInt(konutNoStr); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Geçersiz konut_no: " + konutNoStr); }

        int yil;
        try { yil = Integer.parseInt(yilStr); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Geçersiz yil: " + yilStr); }

        Integer ay = null;
        if (!ayStr.isEmpty()) {
            try {
                ay = Integer.parseInt(ayStr);
                if (ay < 1 || ay > 12) throw new IllegalArgumentException("ay 1-12 arasında olmalıdır.");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Geçersiz ay: " + ayStr);
            }
        }

        BigDecimal tutar;
        try {
            tutar = new BigDecimal(tutarStr.replace(",", "."));
            if (tutar.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("tutar sıfırdan büyük olmalıdır.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Geçersiz tutar: " + tutarStr);
        }

        LocalDate sonOdemeTarihi;
        try {
            sonOdemeTarihi = LocalDate.parse(sonOdemeTarihiStr, DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Geçersiz son_odeme_tarihi (beklenen: yyyy-MM-dd): " + sonOdemeTarihiStr);
        }

        Property property = propertyRepository.findByNumber(konutNo)
                .orElseThrow(() -> new IllegalArgumentException("Konut bulunamadı: " + konutNo));

        upsertDue(property, yil, ay, tutar, sonOdemeTarihi, aciklama, batch, importer);
    }

    private void upsertDue(Property property, int yil, Integer ay, BigDecimal tutar,
                           LocalDate dueDate, String aciklama, DueImport batch, User importer) {
        Optional<Due> existing;
        if (ay != null) {
            existing = dueRepository.findByPropertyIdAndYearAndMonth(property.getId(), yil, ay);
        } else {
            existing = dueRepository.findByPropertyIdAndYearAndNullMonth(property.getId(), yil);
        }

        Due due = existing.orElseGet(Due::new);
        due.setProperty(property);
        due.setPeriodYear(yil);
        due.setPeriodMonth(ay);
        due.setAmount(tutar);
        due.setDueDate(dueDate);
        due.setDescription(aciklama);
        due.setImportBatch(batch);
        if (due.getCreatedBy() == null) {
            due.setCreatedBy(importer);
        }
        // Mevcut ödemeleri koruyarak status'ü yeniden hesapla
        due.recalculateStatus();
        dueRepository.save(due);
    }

    // ── Genel yardımcılar ─────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EkokoyException("FILE_EMPTY", "Dosya boş.", 422);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new EkokoyException("FILE_TOO_LARGE", "Dosya boyutu 5 MB'ı aşamaz.", 422);
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!name.endsWith(".csv") && !name.endsWith(".xlsx")) {
            throw new EkokoyException("INVALID_FILE_TYPE", "Yalnızca CSV veya XLSX dosyası yüklenebilir.", 422);
        }
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private UUID currentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private String serializeErrors(List<Map<String, Object>> errors) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) sb.append(",");
            Map<String, Object> e = errors.get(i);
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : e.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":\"")
                        .append(String.valueOf(entry.getValue()).replace("\"", "\\\"")).append("\"");
                first = false;
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }
}
