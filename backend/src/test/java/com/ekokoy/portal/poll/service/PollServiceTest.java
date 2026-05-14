package com.ekokoy.portal.poll.service;

import com.ekokoy.portal.common.EkokoyException;
import com.ekokoy.portal.poll.dto.*;
import com.ekokoy.portal.poll.entity.*;
import com.ekokoy.portal.poll.repository.*;
import com.ekokoy.portal.user.entity.Role;
import com.ekokoy.portal.user.entity.User;
import com.ekokoy.portal.user.entity.UserRole;
import com.ekokoy.portal.user.entity.UserStatus;
import com.ekokoy.portal.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock private PollRepository pollRepository;
    @Mock private PollQuestionRepository questionRepository;
    @Mock private PollOptionRepository optionRepository;
    @Mock private PollResponseRepository responseRepository;
    @Mock private UserRepository userRepository;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private PollService pollService;

    private final UUID userId  = UUID.randomUUID();
    private final UUID pollId  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(userId.toString());
        lenient().when(authentication.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_EV_SAHIBI")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── createPoll ────────────────────────────────────────────────────────────────

    @Test
    void should_create_poll_with_draft_status() {
        User creator = makeUser();
        when(userRepository.getReferenceById(userId)).thenReturn(creator);
        Poll saved = makePoll(PollStatus.draft);
        when(pollRepository.save(any())).thenReturn(saved);
        when(pollRepository.findById(any())).thenReturn(Optional.of(saved));

        CreatePollRequest req = new CreatePollRequest(
                PollType.vote, "Test Anket", null, false,
                List.of("EV_SAHIBI"), Instant.now().plusSeconds(3600), null,
                List.of(new CreateQuestionRequest("Soru?", QuestionType.yes_no, true, 0,
                        List.of(new CreateOptionRequest("Evet", 0), new CreateOptionRequest("Hayır", 1)))));

        PollDetailResponse result = pollService.createPoll(req);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(PollStatus.draft);
        verify(pollRepository, atLeastOnce()).save(any());
    }

    // ── updatePoll ────────────────────────────────────────────────────────────────

    @Test
    void should_update_draft_poll() {
        Poll poll = makePoll(PollStatus.draft);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollRepository.save(any())).thenReturn(poll);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        UpdatePollRequest req = new UpdatePollRequest(
                PollType.survey, "Güncellendi", "açıklama", true,
                List.of("KIRACI"), Instant.now().plusSeconds(7200), null,
                List.of(new CreateQuestionRequest("Soru 2?", QuestionType.text, true, 0, null)));

        PollDetailResponse result = pollService.updatePoll(pollId, req);
        assertThat(result).isNotNull();
    }

    @Test
    void should_throw_when_updating_non_draft_poll() {
        Poll poll = makePoll(PollStatus.active);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        UpdatePollRequest req = new UpdatePollRequest(
                PollType.vote, "X", null, false, List.of("EV_SAHIBI"),
                Instant.now(), null,
                List.of(new CreateQuestionRequest("S?", QuestionType.yes_no, true, 0, null)));

        assertThatThrownBy(() -> pollService.updatePoll(pollId, req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("POLL_NOT_DRAFT");
    }

    // ── activatePoll ──────────────────────────────────────────────────────────────

    @Test
    void should_activate_draft_poll() {
        Poll poll = makePoll(PollStatus.draft);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollRepository.save(any())).thenReturn(poll);

        PollDetailResponse result = pollService.activatePoll(pollId);
        assertThat(result.status()).isEqualTo(PollStatus.active);
    }

    @Test
    void should_throw_when_activating_non_draft_poll() {
        Poll poll = makePoll(PollStatus.active);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.activatePoll(pollId))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("POLL_NOT_DRAFT");
    }

    // ── closePoll ─────────────────────────────────────────────────────────────────

    @Test
    void should_close_active_poll() {
        Poll poll = makePoll(PollStatus.active);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser());
        when(pollRepository.save(any())).thenReturn(poll);

        PollDetailResponse result = pollService.closePoll(pollId);
        assertThat(result.status()).isEqualTo(PollStatus.closed);
    }

    @Test
    void should_throw_when_closing_non_active_poll() {
        Poll poll = makePoll(PollStatus.draft);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.closePoll(pollId))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("POLL_NOT_ACTIVE");
    }

    // ── cancelPoll ────────────────────────────────────────────────────────────────

    @Test
    void should_cancel_poll() {
        Poll poll = makePoll(PollStatus.active);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser());
        when(pollRepository.save(any())).thenReturn(poll);

        PollDetailResponse result = pollService.cancelPoll(pollId);
        assertThat(result.status()).isEqualTo(PollStatus.cancelled);
    }

    @Test
    void should_throw_when_cancelling_already_cancelled_poll() {
        Poll poll = makePoll(PollStatus.cancelled);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.cancelPoll(pollId))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("ALREADY_CANCELLED");
    }

    // ── respond ───────────────────────────────────────────────────────────────────

    @Test
    void should_reject_response_when_poll_not_active() {
        Poll poll = makePoll(PollStatus.draft);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        SubmitResponseRequest req = new SubmitResponseRequest(List.of());
        assertThatThrownBy(() -> pollService.respond(pollId, req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("POLL_NOT_ACTIVE");
    }

    @Test
    void should_reject_duplicate_response() {
        Poll poll = makePoll(PollStatus.active);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(responseRepository.existsByPollIdAndUserId(pollId, userId)).thenReturn(true);

        SubmitResponseRequest req = new SubmitResponseRequest(List.of());
        assertThatThrownBy(() -> pollService.respond(pollId, req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("ALREADY_RESPONDED");
    }

    @Test
    void should_reject_response_when_user_not_eligible() {
        Poll poll = makePollWithRoles(PollStatus.active, new String[]{"YONETIM_KURULU"});
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        SubmitResponseRequest req = new SubmitResponseRequest(List.of());
        assertThatThrownBy(() -> pollService.respond(pollId, req))
                .isInstanceOf(EkokoyException.class)
                .extracting("code").isEqualTo("FORBIDDEN");
    }

    @Test
    void should_accept_valid_text_response() {
        UUID questionId = UUID.randomUUID();
        Poll poll = makePollWithQuestion(questionId, QuestionType.text);
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(responseRepository.existsByPollIdAndUserId(pollId, userId)).thenReturn(false);
        when(userRepository.getReferenceById(userId)).thenReturn(makeUser());
        when(responseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmitResponseRequest req = new SubmitResponseRequest(
                List.of(new AnswerRequest(questionId, null, null, "Görüşüm")));

        assertThatCode(() -> pollService.respond(pollId, req)).doesNotThrowAnyException();
        verify(responseRepository, times(1)).save(any());
    }

    // ── scheduler ─────────────────────────────────────────────────────────────────

    @Test
    void should_activate_scheduled_polls() {
        Poll poll = makePoll(PollStatus.draft);
        when(pollRepository.findDraftPollsReadyToActivate(any())).thenReturn(List.of(poll));

        pollService.activateScheduledPolls();

        assertThat(poll.getStatus()).isEqualTo(PollStatus.active);
        verify(pollRepository).saveAll(anyList());
    }

    @Test
    void should_close_expired_polls() {
        Poll poll = makePoll(PollStatus.active);
        when(pollRepository.findActivePollsReadyToClose(any())).thenReturn(List.of(poll));

        pollService.closeExpiredPolls();

        assertThat(poll.getStatus()).isEqualTo(PollStatus.closed);
        verify(pollRepository).saveAll(anyList());
    }

    @Test
    void should_not_save_when_no_polls_to_process() {
        when(pollRepository.findDraftPollsReadyToActivate(any())).thenReturn(List.of());
        when(pollRepository.findActivePollsReadyToClose(any())).thenReturn(List.of());

        pollService.activateScheduledPolls();
        pollService.closeExpiredPolls();

        verify(pollRepository, never()).saveAll(anyList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Poll makePoll(PollStatus status) {
        return makePollWithRoles(status, new String[]{"EV_SAHIBI"});
    }

    private Poll makePollWithRoles(PollStatus status, String[] roles) {
        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setType(PollType.vote);
        poll.setTitle("Test Anket");
        poll.setStatus(status);
        poll.setAnonymous(false);
        poll.setEligibleRoles(roles);
        poll.setStartsAt(Instant.now().minusSeconds(60));
        poll.setCreatedBy(makeUser());
        return poll;
    }

    private Poll makePollWithQuestion(UUID questionId, QuestionType questionType) {
        Poll poll = makePoll(PollStatus.active);
        PollQuestion q = new PollQuestion();
        q.setId(questionId);
        q.setPoll(poll);
        q.setQuestionText("Soru?");
        q.setQuestionType(questionType);
        q.setRequired(true);
        q.setQuestionOrder(0);
        q.setOptions(new ArrayList<>());
        poll.setQuestions(List.of(q));
        return poll;
    }

    private User makeUser() {
        User u = new User();
        u.setId(userId);
        u.setFirstName("Test");
        u.setLastName("Kullanıcı");
        u.setEmail("test@ekokoy.com");
        u.setStatus(UserStatus.active);

        Role role = new Role();
        role.setCode("EV_SAHIBI");
        role.setDisplayName("Ev Sahibi");

        UserRole ur = new UserRole();
        ur.setUser(u);
        ur.setRole(role);
        u.setUserRoles(List.of(ur));
        return u;
    }
}
