# SPEC-13 — Oylama & Anket

**Faz:** 1 | **Paket:** `poll/` | **Bağımlılık:** SPEC-02, 04

## Veri Modeli

### polls
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| type | ENUM | vote\|survey |
| title | VARCHAR(255) | NOT NULL |
| description | TEXT | nullable |
| status | ENUM | draft\|active\|closed\|cancelled |
| is_anonymous | BOOLEAN | NOT NULL |
| eligible_roles | VARCHAR[] | NOT NULL |
| starts_at | TIMESTAMP | NOT NULL |
| ends_at | TIMESTAMP | nullable |
| created_by | UUID → users | NOT NULL |
| closed_by | UUID → users | nullable |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### poll_questions
| Alan | Tip |
|------|-----|
| id | UUID PK |
| poll_id | UUID → polls |
| question_text | TEXT |
| question_type | ENUM: yes_no\|single_choice\|multiple_choice\|text |
| is_required | BOOLEAN |
| question_order | INTEGER |
| created_at | TIMESTAMP |

### poll_options
| Alan | Tip |
|------|-----|
| id | UUID PK |
| question_id | UUID → poll_questions |
| option_text | VARCHAR(500) |
| option_order | INTEGER |
| created_at | TIMESTAMP |

### poll_responses
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| poll_id | UUID → polls | NOT NULL |
| question_id | UUID → poll_questions | NOT NULL |
| user_id | UUID → users | her zaman saklanır (audit) |
| option_id | UUID → poll_options | nullable |
| text_answer | TEXT | nullable (type=text için) |
| created_at | TIMESTAMP | |

## İş Kuralları
- Kullanıcı başına poll başına bir yanıt — değiştirilemez
- is_anonymous=true: sonuçlarda user_id gösterilmez (ama DB'de saklanır)
- SUPER_ADMIN ham veriye erişebilir
- Sadece draft güncellenir — active değiştirilemez
- starts_at/@Scheduled → active, ends_at/@Scheduled → closed

## API Endpoints
```
GET    /api/v1/polls                        # authenticated
GET    /api/v1/polls/:id                    # authenticated
GET    /api/v1/polls/:id/results            # eligible_roles
POST   /api/v1/polls/:id/respond            # eligible_roles, bir kez
POST   /api/v1/admin/polls                  # ADMIN, YK
PUT    /api/v1/admin/polls/:id              # ADMIN, YK (sadece draft)
PATCH  /api/v1/admin/polls/:id/activate     # ADMIN, YK
PATCH  /api/v1/admin/polls/:id/close        # ADMIN, YK
PATCH  /api/v1/admin/polls/:id/cancel       # SUPER_ADMIN
GET    /api/v1/admin/polls/:id/results/full # ADMIN, YK
GET    /api/v1/admin/polls/:id/participants # ADMIN, YK (oy vermeyenler)
```

## Kabul Kriterleri
- [ ] vote + survey modları çalışıyor
- [ ] eligible_roles kısıtı uygulanıyor
- [ ] İki kez yanıt engelleniyor
- [ ] is_anonymous: sonuçta isim görünmüyor
- [ ] starts_at/ends_at otomasyonu çalışıyor
- [ ] Katılım istatistikleri doğru hesaplanıyor
- [ ] Oy vermeyenler listesi alınabiliyor
