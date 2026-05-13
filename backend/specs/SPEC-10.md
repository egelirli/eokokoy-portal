# SPEC-10 — Bildirim Servisi

**Faz:** 2 | **Paket:** `notification/` | **Bağımlılık:** SPEC-01..06

## Veri Modeli

### notifications (uygulama içi)
| Alan | Tip |
|------|-----|
| id | UUID PK |
| user_id | UUID → users |
| type | VARCHAR(100) (ör: task.assigned) |
| title | VARCHAR(255) |
| body | TEXT nullable |
| entity_type | VARCHAR(50) nullable |
| entity_id | UUID nullable |
| is_read | BOOLEAN DEFAULT false |
| read_at | TIMESTAMP nullable |
| created_at | TIMESTAMP |

### notification_preferences
| Alan | Tip |
|------|-----|
| id | UUID PK |
| user_id | UUID → users |
| event_type | VARCHAR(100) |
| in_app | BOOLEAN DEFAULT true |
| email | BOOLEAN DEFAULT true |
| whatsapp | BOOLEAN DEFAULT false |
| updated_at | TIMESTAMP |

## Kanallar
| Kanal | Teknoloji | Gönderim |
|-------|-----------|---------|
| Uygulama içi | DB tabanlı | Senkron |
| E-posta | Spring Mail + Thymeleaf | @Async, 3x retry |
| WhatsApp | Meta Cloud API + onaylı şablonlar | @Async, silent fail |

## Zorunlu Bildirimler (tercih atlanır)
- `announcement.urgent` → tüm kanallar
- `auth.password_changed` → in_app + email
- `user.approved` → in_app + email

## Temel Event Tipleri
```
announcement.published, announcement.urgent
task.created, task.assigned, task.completed, task.sla_exceeded
message.received, message.group_added
forum.reply_received
dues.reminder_3days, dues.overdue, dues.payment_recorded
poll.started, poll.reminder, poll.closed
auth.password_changed, auth.new_login, user.approved
```

## Mimari
```
TetikleyiciModül → NotificationService
  → InAppProvider (her zaman)
  → EmailProvider (@Async, tercih kontrolü)
  → WhatsAppProvider (@Async, opt-in + phone kontrolü)
```

## İş Kuralları
- Kullanıcı status!=active → bildirim gönderilmez
- WhatsApp: phone dolu + opt-in = true olmalı
- WhatsApp şablonları Meta tarafından önceden onaylanmalı
- 30 günden eski okunmuş bildirimler @Scheduled ile hard-delete
- Tercihler kullanıcı kaydında varsayılan olarak oluşturulur

## API Endpoints
```
GET    /api/v1/notifications                     # authenticated
GET    /api/v1/notifications/unread-count        # authenticated
PATCH  /api/v1/notifications/:id/read            # authenticated
PATCH  /api/v1/notifications/read-all            # authenticated
GET    /api/v1/notification-preferences          # authenticated
PATCH  /api/v1/notification-preferences/:type    # authenticated
```

## Kabul Kriterleri
- [ ] Uygulama içi bildirim oluşuyor ve okunuyor
- [ ] E-posta gönderimi çalışıyor (retry dahil)
- [ ] WhatsApp opt-in kontrolü çalışıyor
- [ ] Urgent duyuru tercih atlanarak tüm kanallardan gidiyor
- [ ] Kullanıcı kaydında varsayılan tercihler oluşuyor
- [ ] 30 günlük temizlik job çalışıyor
