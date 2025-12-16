package com.outreach.campaign.application.execution;

import com.outreach.campaign.application.lead.CampaignLeadService;
import com.outreach.campaign.application.rendering.EmailRenderService;
import com.outreach.campaign.domain.entities.*;
import com.outreach.campaign.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignExecutionServiceTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CampaignLeadService campaignLeadService;
    @Mock
    private EmailRenderService emailRenderService;
    @Mock
    private EmailSendingService emailSendingService;
    @Mock
    private MailboxRepository mailboxRepository;
    @Mock
    private CampaignMailboxRepository campaignMailboxRepository;
    @Mock
    private SentEmailRepository sentEmailRepository;

    @Spy
    @InjectMocks
    private CampaignExecutionService campaignExecutionService;

    private CampaignEntity campaign;
    private List<LeadEntity> leads;
    private List<CampaignMailboxEntity> campaignMailboxes;
    private MailboxEntity mailbox;

    @BeforeEach
    void setUp() {
        // Setup default campaign
        campaign = new CampaignEntity();
        campaign.setId(1);
        campaign.setStatus("draft");
        campaign.setEmailSubject("Hello {{first_name}}");
        campaign.setEmailBody("Body");
        campaign.setEmailDelayMinutes(0); // No delay for tests

        // Setup leads
        leads = new ArrayList<>();
        LeadEntity lead = new LeadEntity();
        lead.setId(100);
        lead.setEmail("test@example.com");
        leads.add(lead);

        // Setup mailboxes
        campaignMailboxes = new ArrayList<>();
        CampaignMailboxEntity cm = new CampaignMailboxEntity();
        cm.setCampaignId(1);
        cm.setMailboxId(10);
        cm.setPriority(1);
        campaignMailboxes.add(cm);

        mailbox = new MailboxEntity();
        mailbox.setId(10);
        mailbox.setEmailAddress("sender@example.com");

        // Common mocks
        lenient().when(campaignRepository.findById(1)).thenReturn(Optional.of(campaign));
        lenient().when(campaignMailboxRepository.findByCampaignIdOrderByPriorityDesc(1)).thenReturn(campaignMailboxes);
        lenient().when(campaignLeadService.getCampaignLeads(1)).thenReturn(leads);
        lenient().when(mailboxRepository.findById(10)).thenReturn(Optional.of(mailbox));
        lenient().when(emailRenderService.renderEmail(anyString(), any(), any(), anyLong())).thenReturn("Rendered Content");
        lenient().when(emailSendingService.sendEmail(any(), anyString(), anyString(), anyString())).thenReturn(true);
        lenient().when(sentEmailRepository.findByCampaignIdAndLeadId(anyInt(), anyInt())).thenReturn(Collections.emptyList());
        lenient().when(sentEmailRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Stub sleep to prevent waiting during tests
        lenient().doNothing().when(campaignExecutionService).sleep(anyLong());
    }

    @Test
    void testExecuteCampaignSuccess() {
        // When
        campaignExecutionService.executeCampaign(1);

        // Then
        assertEquals("completed", campaign.getStatus());
        verify(emailSendingService, times(1)).sendEmail(any(), eq("test@example.com"), anyString(), anyString());
        verify(sentEmailRepository, times(2)).save(any(SentEmailEntity.class)); // 1 queued, 1 sent
    }

    @Test
    void testExecuteCampaignNotFound() {
        // Given
        when(campaignRepository.findById(99)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> campaignExecutionService.executeCampaign(99));
    }

    @Test
    void testExecuteCampaignNoTemplates() {
        // Given
        campaign.setEmailSubject(null);

        // When
        campaignExecutionService.executeCampaign(1);

        // Then
        assertEquals("draft", campaign.getStatus());
        verify(emailSendingService, never()).sendEmail(any(), any(), any(), any());
    }

    @Test
    void testExecuteCampaignNoMailboxes() {
        // Given
        when(campaignMailboxRepository.findByCampaignIdOrderByPriorityDesc(1)).thenReturn(Collections.emptyList());

        // When
        campaignExecutionService.executeCampaign(1);

        // Then
        assertEquals("draft", campaign.getStatus());
        verify(emailSendingService, never()).sendEmail(any(), any(), any(), any());
    }

    @Test
    void testExecuteCampaignNoLeads() {
        // Given
        when(campaignLeadService.getCampaignLeads(1)).thenReturn(Collections.emptyList());

        // When
        campaignExecutionService.executeCampaign(1);

        // Then
        assertEquals("completed", campaign.getStatus());
        verify(emailSendingService, never()).sendEmail(any(), any(), any(), any());
    }

    @Test
    void testExecuteCampaignAlreadySent() {
        // Given
        SentEmailEntity sent = new SentEmailEntity();
        sent.setStatus("sent");
        when(sentEmailRepository.findByCampaignIdAndLeadId(1, 100)).thenReturn(Collections.singletonList(sent));

        // When
        campaignExecutionService.executeCampaign(1);

        // Then
        verify(emailSendingService, never()).sendEmail(any(), any(), any(), any());
        assertEquals("completed", campaign.getStatus()); // Should be completed if all skipped
        // Also verify no failures logged
        assertEquals("sent", sent.getStatus()); // Sent entity unmodified
    }

    @Test
    void testExecuteCampaignMailboxSelection() {
        // Given
        CampaignMailboxEntity cm1 = new CampaignMailboxEntity();
        cm1.setMailboxId(10);
        cm1.setCampaignId(1);
        CampaignMailboxEntity cm2 = new CampaignMailboxEntity();
        cm2.setMailboxId(11);
        cm2.setCampaignId(1);
        List<CampaignMailboxEntity> mbs = Arrays.asList(cm1, cm2);
        
        when(campaignMailboxRepository.findByCampaignIdOrderByPriorityDesc(1)).thenReturn(mbs);
        
        MailboxEntity m1 = new MailboxEntity(); m1.setId(10);
        MailboxEntity m2 = new MailboxEntity(); m2.setId(11);
        lenient().when(mailboxRepository.findById(10)).thenReturn(Optional.of(m1));
        lenient().when(mailboxRepository.findById(11)).thenReturn(Optional.of(m2));
        
        LeadEntity l1 = new LeadEntity(); l1.setId(101); l1.setEmail("l1@e.com");
        LeadEntity l2 = new LeadEntity(); l2.setId(102); l2.setEmail("l2@e.com");
        when(campaignLeadService.getCampaignLeads(1)).thenReturn(Arrays.asList(l1, l2));

        // When
        campaignExecutionService.executeCampaign(1);

        // Then
        ArgumentCaptor<MailboxEntity> mailboxCaptor = ArgumentCaptor.forClass(MailboxEntity.class);
        verify(emailSendingService, times(2)).sendEmail(mailboxCaptor.capture(), anyString(), anyString(), anyString());
        
        List<MailboxEntity> usedMailboxes = mailboxCaptor.getAllValues();
        assertEquals(2, usedMailboxes.size());
        // Simple round robin implies different mailboxes or alternating
        // Implementation uses hashmap index, initial 0.
        // Lead 1 -> index 0 -> m1
        // Lead 2 -> index 1 -> m2
        assertNotEquals(usedMailboxes.get(0).getId(), usedMailboxes.get(1).getId());
    }

    @Test
    void testExecuteCampaignSendFailure() {
        // Given
        when(emailSendingService.sendEmail(any(), any(), any(), any())).thenReturn(false);

        // When
        campaignExecutionService.executeCampaign(1);

        // Then
        verify(sentEmailRepository, atLeastOnce()).save(argThat(s -> "failed".equals(s.getStatus())));
        assertEquals("failed", campaign.getStatus());
    }

    @Test
    void testExecuteCampaignExceptionHandling() {
        // Given
        when(emailSendingService.sendEmail(any(), any(), any(), any())).thenThrow(new RuntimeException("Unexpected"));

        // When
        campaignExecutionService.executeCampaign(1);

        // Then
        // Should catch exception and continue/fail count
        assertEquals("failed", campaign.getStatus());
    }
}
