import { useEffect, useRef } from "react";
import EmailSettingsSidebar from "./EmailSettingsSidebar";
import EmailComposeArea from "./EmailComposeArea";

export default function Step2ComposeEmail({
  emailSubject,
  setEmailSubject,
  emailContent,
  setEmailContent,
  emailVariants,
  setEmailVariants,
  followUps,
  setFollowUps,
  selectedFollowUp,
  setSelectedFollowUp,
  selectedFollowUpVariant,
  setSelectedFollowUpVariant,
  followUpTiming,
  setFollowUpTiming,
  campaignName,
  campaignId,
  contactId
}) {
  const isSwitchingRef = useRef(false);
  const variantsRef = useRef(emailVariants);

  // Update ref when variants change (but not from auto-save)
  useEffect(() => {
    if (!isSwitchingRef.current) {
      variantsRef.current = emailVariants;
    }
  }, [emailVariants]);

  // Auto-save to active variant whenever subject or content changes
  useEffect(() => {
    // Skip auto-save if we're currently switching variants
    if (isSwitchingRef.current) {
      isSwitchingRef.current = false;
      return;
    }

    // Handle Follow-up variants first (if selected)
    if (selectedFollowUp && selectedFollowUpVariant) {
      const followUp = followUps.find(f => f.id === selectedFollowUp);
      if (followUp && followUp.variants) {
        const activeFollowUpVariant = followUp.variants.find(v => v.id === selectedFollowUpVariant);
        if (activeFollowUpVariant && (activeFollowUpVariant.subject !== emailSubject || activeFollowUpVariant.content !== emailContent)) {
          setFollowUps(prevFollowUps =>
            prevFollowUps.map(f =>
              f.id === selectedFollowUp
                ? {
                    ...f,
                    variants: f.variants.map(v =>
                      v.id === selectedFollowUpVariant
                        ? { ...v, subject: emailSubject, content: emailContent }
                        : v
                    )
                  }
                : f
            )
          );
          return; // Don't save to Initial Contact if we're editing a follow-up
        }
      }
    }

    // Handle Initial Contact variants (only if no follow-up is selected)
    const activeVariant = variantsRef.current.find(v => v.isActive);
    if (activeVariant && (activeVariant.subject !== emailSubject || activeVariant.content !== emailContent)) {
      setEmailVariants(prevVariants =>
        prevVariants.map(v =>
          v.id === activeVariant.id
            ? { ...v, subject: emailSubject, content: emailContent }
            : v
        )
      );
    }
  }, [emailSubject, emailContent, setEmailVariants, selectedFollowUp, selectedFollowUpVariant, followUps, setFollowUps]);

  return (
    <div className="flex gap-6">
      {/* Email Settings Sidebar */}
      <EmailSettingsSidebar
        emailVariants={emailVariants}
        setEmailVariants={setEmailVariants}
        emailSubject={emailSubject}
        setEmailSubject={setEmailSubject}
        emailContent={emailContent}
        setEmailContent={setEmailContent}
        followUps={followUps}
        setFollowUps={setFollowUps}
        selectedFollowUp={selectedFollowUp}
        setSelectedFollowUp={setSelectedFollowUp}
        selectedFollowUpVariant={selectedFollowUpVariant}
        setSelectedFollowUpVariant={setSelectedFollowUpVariant}
        followUpTiming={followUpTiming}
        setFollowUpTiming={setFollowUpTiming}
        isSwitchingRef={isSwitchingRef}
        variantsRef={variantsRef}
      />

      {/* Email Compose Area */}
      <EmailComposeArea
        emailSubject={emailSubject}
        setEmailSubject={setEmailSubject}
        emailContent={emailContent}
        setEmailContent={setEmailContent}
        campaignName={campaignName}
        campaignContext={{ campaignName }} // TODO: Expand with more context (industry, target audience, etc.)
        campaignId={campaignId}
        contactId={contactId}
      />
    </div>
  );
}

