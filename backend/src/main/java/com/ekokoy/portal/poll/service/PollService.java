package com.ekokoy.portal.poll.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.poll.dto.*;
import com.ekokoy.portal.poll.entity.*;
import com.ekokoy.portal.poll.repository.*;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollQuestionRepository questionRepository;
    private final PollOptionRepository optionRepository;
    private final PollResponseRepository responseRepository;
    private final UserRepository userRepository;

    public PollService(PollRepository pollRepository,
                       PollQuestionRepository questionRepository,
                       PollOptionRepository optionRepository,
                       PollResponseRepository responseRepository,
                       UserRepository userRepository) {
        this.pollRepository = pollRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.responseRepository = responseRepository;
        this.userRepository = userRepository;
    }

    // ── Admin: CRUD ───────────────────────────────────────────────────────────────

    /** Yeni anket oluşturur (ADMIN, YK). */
    @Transactional
    public PollDetailResponse createPoll(CreatePollRequest req) {
        User creator = requireCurrentUser();
        Poll poll = new Poll();
        applyPollFields(poll, req.type(), req.title(), req.description(),
                req.isAnonymous(), req.eligibleRoles(), req.startsAt(), req.endsAt());
        poll.setCreatedBy(creator);
        pollRepository.save(poll);
        saveQuestions(poll, req.questions());
        Poll saved = requirePoll(poll.getId());
        return PollDetailResponse.from(saved, false);
    }

    /** Draft anketi günceller (ADMIN, YK). */
    @Transactional
    public PollDetailResponse updatePoll(UUID pollId, UpdatePollRequest req) {
        Poll poll = requirePoll(pollId);
        if (poll.getStatus() != PollStatus.draft) {
            throw new EkokoyException("POLL_NOT_DRAFT", "Sadece taslak anketler güncellenebilir.", 422);
        }
        applyPollFields(poll, req.type(), req.title(), req.description(),
                req.isAnonymous(), req.eligibleRoles(), req.startsAt(), req.endsAt());
        poll.getQuestions().clear();
        pollRepository.save(poll);
        saveQuestions(poll, req.questions());
        Poll updated = requirePoll(pollId);
        return PollDetailResponse.from(updated, false);
    }

    /** Anketi aktifleştirir (ADMIN, YK). */
    @Transactional
    public PollDetailResponse activatePoll(UUID pollId) {
        Poll poll = requirePoll(pollId);
        if (poll.getStatus() != PollStatus.draft) {
            throw new EkokoyException("POLL_NOT_DRAFT", "Sadece taslak anketler aktifleştirilebilir.", 422);
        }
        poll.setStatus(PollStatus.active);
        pollRepository.save(poll);
        return PollDetailResponse.from(poll, false);
    }

    /** Anketi kapatır (ADMIN, YK). */
    @Transactional
    public PollDetailResponse closePoll(UUID pollId) {
        Poll poll = requirePoll(pollId);
        if (poll.getStatus() != PollStatus.active) {
            throw new EkokoyException("POLL_NOT_ACTIVE", "Sadece aktif anketler kapatılabilir.", 422);
        }
        User closer = requireCurrentUser();
        poll.setStatus(PollStatus.closed);
        poll.setClosedBy(closer);
        pollRepository.save(poll);
        return PollDetailResponse.from(poll, false);
    }

    /** Anketi iptal eder (SUPER_ADMIN). */
    @Transactional
    public PollDetailResponse cancelPoll(UUID pollId) {
        Poll poll = requirePoll(pollId);
        if (poll.getStatus() == PollStatus.cancelled) {
            throw new EkokoyException("ALREADY_CANCELLED", "Anket zaten iptal edilmiş.", 422);
        }
        User closer = requireCurrentUser();
        poll.setStatus(PollStatus.cancelled);
        poll.setClosedBy(closer);
        pollRepository.save(poll);
        return PollDetailResponse.from(poll, false);
    }

    /** Tüm anketleri listeler (ADMIN, YK). */
    @Transactional(readOnly = true)
    public List<PollSummaryResponse> listAllPolls() {
        UUID currentUserId = currentUserId();
        return pollRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> PollSummaryResponse.from(p, responseRepository.existsByPollIdAndUserId(p.getId(), currentUserId)))
                .toList();
    }

    /** Anketin tam sonuçlarını döner (ADMIN, YK) — anonim olmayan anketlerde user_id gösterilir. */
    @Transactional(readOnly = true)
    public PollResultResponse getFullResults(UUID pollId) {
        Poll poll = requirePoll(pollId);
        return buildResults(poll, false);
    }

    /** Ankete katılanlar ve katılmayanlar listesi (ADMIN, YK). */
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(UUID pollId) {
        Poll poll = requirePoll(pollId);
        List<UUID> voterIds = responseRepository.findDistinctUserIdsByPollId(pollId);
        Set<UUID> voterSet = new HashSet<>(voterIds);

        List<User> eligibleUsers = findEligibleUsers(poll.getEligibleRoles());
        return eligibleUsers.stream()
                .map(u -> new ParticipantResponse(
                        u.getId(), u.getFirstName(), u.getLastName(),
                        u.getEmail(), voterSet.contains(u.getId())))
                .sorted(Comparator.comparing(ParticipantResponse::hasVoted)
                        .thenComparing(ParticipantResponse::lastName))
                .toList();
    }

    // ── User: Anket listesi ve yanıt ─────────────────────────────────────────────

    /** Giriş yapan kullanıcıya ait aktif anketleri listeler. */
    @Transactional(readOnly = true)
    public List<PollSummaryResponse> listMyPolls() {
        UUID userId = currentUserId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return pollRepository.findByStatus(PollStatus.active).stream()
                .filter(p -> isEligible(p, auth))
                .map(p -> PollSummaryResponse.from(p,
                        responseRepository.existsByPollIdAndUserId(p.getId(), userId)))
                .toList();
    }

    /** Anket detayını döner (kimlik doğrulama gerekli). */
    @Transactional(readOnly = true)
    public PollDetailResponse getPoll(UUID pollId) {
        Poll poll = requirePoll(pollId);
        UUID userId = currentUserId();
        boolean hasResponded = responseRepository.existsByPollIdAndUserId(pollId, userId);
        return PollDetailResponse.from(poll, hasResponded);
    }

    /** Anket sonuçlarını döner — eligible_roles kısıtı uygulanır; anonim ankette user_id maskelenir. */
    @Transactional(readOnly = true)
    public PollResultResponse getResults(UUID pollId) {
        Poll poll = requirePoll(pollId);
        checkEligibleRoles(poll);
        return buildResults(poll, poll.isAnonymous());
    }

    /** Ankete yanıt gönderir — yalnızca bir kez. */
    @Transactional
    public void respond(UUID pollId, SubmitResponseRequest req) {
        Poll poll = requirePoll(pollId);
        if (poll.getStatus() != PollStatus.active) {
            throw new EkokoyException("POLL_NOT_ACTIVE", "Bu anket aktif değil.", 422);
        }
        checkEligibleRoles(poll);

        UUID userId = currentUserId();
        if (responseRepository.existsByPollIdAndUserId(pollId, userId)) {
            throw new EkokoyException("ALREADY_RESPONDED", "Bu ankete daha önce katıldınız.", 422);
        }

        User user = userRepository.getReferenceById(userId);
        Map<UUID, PollQuestion> questionMap = poll.getQuestions().stream()
                .collect(Collectors.toMap(PollQuestion::getId, q -> q));

        for (AnswerRequest answer : req.answers()) {
            PollQuestion question = questionMap.get(answer.questionId());
            if (question == null) {
                throw new EkokoyException("INVALID_QUESTION",
                        "Geçersiz soru ID: " + answer.questionId(), 422);
            }
            saveAnswerRows(poll, question, user, answer);
        }
    }

    // ── Scheduler ────────────────────────────────────────────────────────────────

    /** starts_at zamanı geçen draft anketleri aktifleştirir. */
    @Transactional
    public void activateScheduledPolls() {
        List<Poll> ready = pollRepository.findDraftPollsReadyToActivate(Instant.now());
        for (Poll poll : ready) {
            poll.setStatus(PollStatus.active);
        }
        if (!ready.isEmpty()) {
            pollRepository.saveAll(ready);
        }
    }

    /** ends_at zamanı geçen aktif anketleri kapatır. */
    @Transactional
    public void closeExpiredPolls() {
        List<Poll> expired = pollRepository.findActivePollsReadyToClose(Instant.now());
        for (Poll poll : expired) {
            poll.setStatus(PollStatus.closed);
        }
        if (!expired.isEmpty()) {
            pollRepository.saveAll(expired);
        }
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────────

    private void applyPollFields(Poll poll, PollType type, String title, String description,
                                 boolean isAnonymous, List<String> eligibleRoles,
                                 Instant startsAt, Instant endsAt) {
        poll.setType(type);
        poll.setTitle(title);
        poll.setDescription(description);
        poll.setAnonymous(isAnonymous);
        poll.setEligibleRoles(eligibleRoles.toArray(new String[0]));
        poll.setStartsAt(startsAt);
        poll.setEndsAt(endsAt);
    }

    private void saveQuestions(Poll poll, List<CreateQuestionRequest> questionRequests) {
        for (CreateQuestionRequest qReq : questionRequests) {
            PollQuestion question = new PollQuestion();
            question.setPoll(poll);
            question.setQuestionText(qReq.questionText());
            question.setQuestionType(qReq.questionType());
            question.setRequired(qReq.isRequired());
            question.setQuestionOrder(qReq.questionOrder());
            questionRepository.save(question);

            if (qReq.options() != null) {
                for (CreateOptionRequest oReq : qReq.options()) {
                    PollOption option = new PollOption();
                    option.setQuestion(question);
                    option.setOptionText(oReq.optionText());
                    option.setOptionOrder(oReq.optionOrder());
                    optionRepository.save(option);
                }
            }
        }
    }

    private void saveAnswerRows(Poll poll, PollQuestion question, User user, AnswerRequest answer) {
        QuestionType type = question.getQuestionType();

        if (type == QuestionType.text) {
            PollResponse row = new PollResponse();
            row.setPoll(poll);
            row.setQuestion(question);
            row.setUser(user);
            row.setTextAnswer(answer.textAnswer());
            responseRepository.save(row);
            return;
        }

        if (type == QuestionType.multiple_choice && answer.optionIds() != null) {
            for (UUID optionId : answer.optionIds()) {
                PollOption option = optionRepository.findById(optionId)
                        .orElseThrow(() -> new EkokoyException("INVALID_OPTION",
                                "Geçersiz seçenek ID: " + optionId, 422));
                PollResponse row = new PollResponse();
                row.setPoll(poll);
                row.setQuestion(question);
                row.setUser(user);
                row.setOption(option);
                responseRepository.save(row);
            }
            return;
        }

        // yes_no veya single_choice
        if (answer.optionId() != null) {
            PollOption option = optionRepository.findById(answer.optionId())
                    .orElseThrow(() -> new EkokoyException("INVALID_OPTION",
                            "Geçersiz seçenek ID: " + answer.optionId(), 422));
            PollResponse row = new PollResponse();
            row.setPoll(poll);
            row.setQuestion(question);
            row.setUser(user);
            row.setOption(option);
            responseRepository.save(row);
        }
    }

    private PollResultResponse buildResults(Poll poll, boolean maskUsers) {
        List<UUID> voterIds = responseRepository.findDistinctUserIdsByPollId(poll.getId());
        long totalParticipants = voterIds.size();
        long totalEligible = findEligibleUsers(poll.getEligibleRoles()).size();
        double participationRate = totalEligible > 0
                ? (double) totalParticipants / totalEligible * 100 : 0;

        List<QuestionResultResponse> questionResults = poll.getQuestions().stream()
                .map(q -> buildQuestionResult(q, maskUsers))
                .toList();

        return new PollResultResponse(
                poll.getId(), poll.getTitle(), poll.getType(), poll.getStatus(),
                poll.isAnonymous(), totalParticipants, totalEligible,
                participationRate, questionResults);
    }

    private QuestionResultResponse buildQuestionResult(PollQuestion question, boolean maskUsers) {
        List<PollResponse> responses = responseRepository.findByQuestionId(question.getId());
        long totalAnswers = responses.size();

        List<OptionResultResponse> optionResults = List.of();
        List<String> textAnswers = List.of();

        if (question.getQuestionType() == QuestionType.text) {
            textAnswers = responses.stream()
                    .map(r -> maskUsers ? r.getTextAnswer() : r.getTextAnswer())
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            Map<UUID, Long> counts = responses.stream()
                    .filter(r -> r.getOption() != null)
                    .collect(Collectors.groupingBy(r -> r.getOption().getId(), Collectors.counting()));

            optionResults = question.getOptions().stream()
                    .map(o -> {
                        long count = counts.getOrDefault(o.getId(), 0L);
                        double pct = totalAnswers > 0 ? (double) count / totalAnswers * 100 : 0;
                        return new OptionResultResponse(o.getId(), o.getOptionText(), count, pct);
                    })
                    .toList();
        }

        return new QuestionResultResponse(
                question.getId(), question.getQuestionText(), question.getQuestionType(),
                question.getQuestionOrder(), totalAnswers, optionResults, textAnswers);
    }

    private void checkEligibleRoles(Poll poll) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isEligible(poll, auth)) {
            throw new EkokoyException("FORBIDDEN", "Bu anket için gerekli role sahip değilsiniz.", 403);
        }
    }

    private boolean isEligible(Poll poll, Authentication auth) {
        if (auth == null) return false;
        return Arrays.stream(poll.getEligibleRoles())
                .anyMatch(role -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)));
    }

    private List<User> findEligibleUsers(String[] eligibleRoles) {
        // Tüm kullanıcıları yükleyip Java tarafında filtrele
        // (Kullanıcı sayısı 94 konut için küçük kalır)
        return userRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .filter(u -> u.getUserRoles().stream()
                        .anyMatch(ur -> Arrays.asList(eligibleRoles)
                                .contains(ur.getRole().getCode())))
                .toList();
    }

    private Poll requirePoll(UUID id) {
        return pollRepository.findById(id)
                .orElseThrow(() -> new EkokoyException("POLL_NOT_FOUND", "Anket bulunamadı: " + id, 404));
    }

    private User requireCurrentUser() {
        return userRepository.getReferenceById(currentUserId());
    }

    private UUID currentUserId() {
        return UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
