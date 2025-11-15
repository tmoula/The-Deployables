import { FileText, Mail } from "lucide-react";
import { useState } from "react";
import DatePicker from "react-datepicker";
import { addDays, format } from "date-fns";

export default function EmailSettingsSidebar({
  emailVariants,
  setEmailVariants,
  emailSubject,
  setEmailSubject,
  emailContent,
  setEmailContent,
  followUps,
  setFollowUps,
  selectedFollowUp,
  setSelectedFollowUp,
  selectedFollowUpVariant,
  setSelectedFollowUpVariant,
  followUpTiming,
  setFollowUpTiming,
  isSwitchingRef,
  variantsRef
}) {
  const campaignStartDate = new Date(); // Default to today
  const [showTimingDatePicker, setShowTimingDatePicker] = useState(null); // null or followUpTiming id or 'initial'

  // Calculate date based on start date and delay days
  const calculateDate = (delayDays, baseDate = null) => {
    const base = baseDate || campaignStartDate;
    return addDays(base, delayDays);
  };

  // Calculate follow-up date (based on previous follow-up or initial contact)
  const calculateFollowUpDate = (followUp, index) => {
    if (index === 0) {
      // First follow-up: based on campaign start date + initial delay
      return calculateDate(followUpTiming?.delayDays || 3);
    } else {
      // Subsequent follow-ups: based on previous follow-up date
      const prevFollowUp = followUps[index - 1];
      const prevDate = calculateFollowUpDate(prevFollowUp, index - 1);
      return calculateDate(followUp.delayDays || 3, prevDate);
    }
  };

  // Format date helper with day of week
  const formatDate = (date) => {
    return format(date, 'EEE, MMM d, yyyy'); // e.g., "Mon, Nov 7, 2024"
  };

  return (
    <div className="w-96 flex-shrink-0 border border-gray-300 rounded-lg shadow-sm bg-white p-6">
      <h3 className="text-xl font-semibold text-gray-800 mb-6">
        Email Settings
      </h3>
      
      <div className="space-y-6">
      {/* Email Variants Section */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
            <FileText size={16} />
            Initial Contact
          </h4>
          <button
            onClick={() => {
              const newVariant = {
                id: emailVariants.length + 1,
                subject: emailSubject,
                content: emailContent,
                isActive: false
              };
              setEmailVariants([...emailVariants, newVariant]);
            }}
            className="text-xs text-blue-600 hover:text-blue-800 font-medium"
          >
            + Add Variant
          </button>
        </div>
        <div className="space-y-2 max-h-64 overflow-y-auto">
          {emailVariants.map((variant, index) => (
            <div 
              key={variant.id} 
              onClick={() => {
                // Clear follow-up selection when switching to Initial Contact variant
                setSelectedFollowUp(null);
                setSelectedFollowUpVariant(null);
                
                // Save current content to previously active variant before switching
                const previouslyActive = emailVariants.find(v => v.isActive);
                let updated = emailVariants.map(v => 
                  v.id === variant.id ? {...v, isActive: true} : {...v, isActive: false}
                );
                
                // Update the previously active variant with current values before switching
                if (previouslyActive && previouslyActive.id !== variant.id) {
                  updated = updated.map(v =>
                    v.id === previouslyActive.id
                      ? {...v, subject: emailSubject, content: emailContent}
                      : v
                  );
                }
                
                isSwitchingRef.current = true; // Prevent auto-save during switch
                setEmailVariants(updated);
                variantsRef.current = updated; // Update ref immediately
                setEmailSubject(variant.subject || "");
                setEmailContent(variant.content || "");
                setTimeout(() => {
                  isSwitchingRef.current = false; // Re-enable auto-save after state updates
                }, 0);
              }}
              className={`border rounded p-2 cursor-pointer transition-colors ${
                variant.isActive 
                  ? 'border-blue-500 bg-blue-50' 
                  : 'border-gray-200 bg-gray-50 hover:bg-gray-100'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-gray-700">Variant {index + 1}</span>
                {emailVariants.length > 1 && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setEmailVariants(emailVariants.filter(v => v.id !== variant.id));
                      if (variant.isActive && emailVariants.length > 1) {
                        const remaining = emailVariants.filter(v => v.id !== variant.id);
                        if (remaining.length > 0) {
                          const first = remaining[0];
                          setEmailSubject(first.subject || "");
                          setEmailContent(first.content || "");
                          setEmailVariants(remaining.map((v, i) => ({...v, isActive: i === 0})));
                        }
                      }
                    }}
                    className="text-xs text-red-600 hover:text-red-800"
                  >
                    ✕
                  </button>
                )}
              </div>
              {variant.subject && (
                <p className="text-xs text-gray-600 mt-1 truncate">{variant.subject}</p>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Follow-up Timing Configuration - Between Initial Contact and First Follow-up */}
      {followUps.length > 0 && (
        <div className="pt-4 border-t border-gray-200">
          <div className="mb-4 flex items-center justify-center">
            <div className="flex items-center gap-3 w-full">
              {/* Left line */}
              <div className="flex-1 h-px bg-gradient-to-r from-transparent via-gray-300 to-gray-400"></div>
              {/* Center timing info - Editable */}
              <div className="flex flex-col items-center gap-1 px-3 py-2 bg-gray-50 rounded-full border border-gray-200 hover:border-blue-300 transition-colors">
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min="1"
                    value={followUpTiming?.delayDays || 3}
                    onChange={(e) =>
                      setFollowUpTiming({
                        ...followUpTiming,
                        delayDays: parseInt(e.target.value) || 3
                      })
                    }
                    className="w-12 text-xs text-gray-600 font-medium bg-transparent border-0 focus:outline-none focus:ring-1 focus:ring-blue-500 rounded px-1 text-center"
                    onClick={(e) => e.stopPropagation()}
                  />
                  <span className="text-xs text-gray-600">days</span>
                </div>
                <button
                  type="button"
                  onClick={() => setShowTimingDatePicker(showTimingDatePicker === 'initial' ? null : 'initial')}
                  className="text-xs text-blue-600 hover:text-blue-800 font-medium cursor-pointer"
                >
                  {formatDate(calculateDate(followUpTiming?.delayDays || 3))}
                </button>
              </div>
              {/* Right line */}
              <div className="flex-1 h-px bg-gradient-to-l from-transparent via-gray-300 to-gray-400"></div>
            </div>
          </div>
          {showTimingDatePicker === 'initial' && (
            <div className="mb-2 flex justify-center">
              <div className="bg-white border border-gray-200 rounded-lg shadow-lg p-2">
                <DatePicker
                  selected={calculateDate(followUpTiming?.delayDays || 3)}
                  onChange={(date) => {
                    const daysDiff = Math.ceil((date - campaignStartDate) / (1000 * 60 * 60 * 24));
                    setFollowUpTiming({
                      ...followUpTiming,
                      delayDays: daysDiff > 0 ? daysDiff : 1
                    });
                    setShowTimingDatePicker(null);
                  }}
                  inline
                  minDate={addDays(campaignStartDate, 1)}
                />
              </div>
            </div>
          )}
        </div>
      )}
      </div>

      {/* Follow-ups Section */}
      <div className="mt-6 pt-6 border-t border-gray-200">
        <div className="flex items-center justify-between mb-4">
          <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
            <Mail size={16} />
            Follow-ups
          </h4>
          <button
            onClick={() => {
              const newFollowUp = {
                id: followUps.length + 1,
                delayDays: followUpTiming?.delayDays || 3,
                fallsOnDay: "",
                variants: [
                  { id: 1, subject: "", content: "", isActive: true }
                ]
              };
              setFollowUps([...followUps, newFollowUp]);
            }}
            className="text-xs text-blue-600 hover:text-blue-800 font-medium"
          >
            + Add Follow-up
          </button>
        </div>
        <div className="space-y-4 max-h-[500px] overflow-y-auto">
          {followUps.map((followUp, index) => (
            <div key={followUp.id} className="relative">
              {/* Smooth Timing Connector - Before each follow-up */}
              {index > 0 && (
                <div className="mb-4 flex items-center justify-center">
                  <div className="flex items-center gap-3 w-full">
                    {/* Left line */}
                    <div className="flex-1 h-px bg-gradient-to-r from-transparent via-gray-300 to-gray-400"></div>
                    {/* Center timing info - Editable */}
                    <div className="flex flex-col items-center gap-1 px-3 py-2 bg-gray-50 rounded-full border border-gray-200 hover:border-blue-300 transition-colors">
                      <div className="flex items-center gap-2">
                        <input
                          type="number"
                          min="1"
                          value={followUp.delayDays || 3}
                          onChange={(e) => {
                            const updated = followUps.map(f =>
                              f.id === followUp.id ? {...f, delayDays: parseInt(e.target.value) || 3} : f
                            );
                            setFollowUps(updated);
                          }}
                          className="w-12 text-xs text-gray-600 font-medium bg-transparent border-0 focus:outline-none focus:ring-1 focus:ring-blue-500 rounded px-1 text-center"
                          onClick={(e) => e.stopPropagation()}
                        />
                        <span className="text-xs text-gray-600">days</span>
                      </div>
                      <button
                        type="button"
                        onClick={() => setShowTimingDatePicker(showTimingDatePicker === followUp.id ? null : followUp.id)}
                        className="text-xs text-blue-600 hover:text-blue-800 font-medium cursor-pointer"
                      >
                        {formatDate(calculateFollowUpDate(followUp, index))}
                      </button>
                    </div>
                    {/* Right line */}
                    <div className="flex-1 h-px bg-gradient-to-l from-transparent via-gray-300 to-gray-400"></div>
                  </div>
                </div>
              )}
              
              {/* Date Picker for this follow-up */}
              {showTimingDatePicker === followUp.id && (
                <div className="mb-2 flex justify-center">
                  <div className="bg-white border border-gray-200 rounded-lg shadow-lg p-2">
                    <DatePicker
                      selected={calculateFollowUpDate(followUp, index)}
                      onChange={(date) => {
                        const prevFollowUp = index > 0 ? followUps[index - 1] : null;
                        const prevDate = prevFollowUp 
                          ? calculateFollowUpDate(prevFollowUp, index - 1)
                          : campaignStartDate;
                        const daysDiff = Math.ceil((date - prevDate) / (1000 * 60 * 60 * 24));
                        const updated = followUps.map(f =>
                          f.id === followUp.id ? {...f, delayDays: daysDiff > 0 ? daysDiff : 1} : f
                        );
                        setFollowUps(updated);
                        setShowTimingDatePicker(null);
                      }}
                      inline
                      minDate={index > 0 && followUps[index - 1] 
                        ? addDays(calculateFollowUpDate(followUps[index - 1], index - 1), 1)
                        : addDays(campaignStartDate, 1)
                      }
                    />
                  </div>
                </div>
              )}
              
              {/* Follow-up Box */}
              <div className="border border-gray-200 rounded p-2 bg-gray-50">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-medium text-gray-700">Follow-up {index + 1}</span>
                  <button
                    onClick={() => {
                      setFollowUps(followUps.filter(f => f.id !== followUp.id));
                      if (selectedFollowUp === followUp.id) {
                        setSelectedFollowUp(null);
                        setSelectedFollowUpVariant(null);
                      }
                    }}
                    className="text-xs text-red-600 hover:text-red-800"
                  >
                    ✕
                  </button>
                </div>
                
                {/* Variants for Follow-up */}
                <div className="mb-2">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs text-gray-600">Variants</span>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        const followUpVariants = followUp.variants || [{ id: 1, subject: "", content: "", isActive: true }];
                        const newVariant = {
                          id: followUpVariants.length + 1,
                          subject: emailSubject,
                          content: emailContent,
                          isActive: false
                        };
                        const updated = followUps.map(f =>
                          f.id === followUp.id
                            ? { ...f, variants: [...followUpVariants, newVariant] }
                            : f
                        );
                        setFollowUps(updated);
                      }}
                      className="text-xs text-blue-600 hover:text-blue-800"
                    >
                      + Add Variant
                    </button>
                  </div>
                  <div className="space-y-1">
                    {(followUp.variants || [{ id: 1, subject: "", content: "", isActive: true }]).map((variant, vIndex) => (
                      <div
                        key={variant.id}
                        onClick={() => {
                          // Save current content to previously selected follow-up variant before switching
                          if (selectedFollowUp && selectedFollowUpVariant) {
                            const prevFollowUp = followUps.find(f => f.id === selectedFollowUp);
                            if (prevFollowUp && prevFollowUp.variants) {
                              const updated = followUps.map(f =>
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
                              );
                              setFollowUps(updated);
                            }
                          }
                          
                          // Clear Initial Contact selection when switching to follow-up variant
                          const updatedInitialContact = emailVariants.map(v => ({...v, isActive: false}));
                          setEmailVariants(updatedInitialContact);
                          
                          setSelectedFollowUp(followUp.id);
                          setSelectedFollowUpVariant(variant.id);
                          setEmailSubject(variant.subject || "");
                          setEmailContent(variant.content || "");
                          const updated = followUps.map(f =>
                            f.id === followUp.id
                              ? {
                                  ...f,
                                  variants: f.variants.map(v =>
                                    v.id === variant.id ? {...v, isActive: true} : {...v, isActive: false}
                                  )
                                }
                              : f
                          );
                          setFollowUps(updated);
                        }}
                        className={`border rounded p-1.5 cursor-pointer transition-colors text-xs ${
                          selectedFollowUp === followUp.id && selectedFollowUpVariant === variant.id
                            ? 'border-blue-500 bg-blue-50'
                            : variant.isActive
                            ? 'border-blue-300 bg-blue-50/50'
                            : 'border-gray-200 bg-white hover:bg-gray-50'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="text-xs text-gray-700">Variant {vIndex + 1}</span>
                          {(followUp.variants || []).length > 1 && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                const updatedVariants = followUp.variants.filter(v => v.id !== variant.id);
                                const updated = followUps.map(f =>
                                  f.id === followUp.id
                                    ? {
                                        ...f,
                                        variants: updatedVariants.length > 0
                                          ? updatedVariants.map((v, i) => ({...v, isActive: i === 0}))
                                          : [{ id: 1, subject: "", content: "", isActive: true }]
                                      }
                                    : f
                                );
                                setFollowUps(updated);
                                if (selectedFollowUp === followUp.id && selectedFollowUpVariant === variant.id) {
                                  if (updatedVariants.length > 0) {
                                    const first = updatedVariants[0];
                                    setEmailSubject(first.subject || "");
                                    setEmailContent(first.content || "");
                                    setSelectedFollowUpVariant(first.id);
                                  } else {
                                    setSelectedFollowUp(null);
                                    setSelectedFollowUpVariant(null);
                                  }
                                }
                              }}
                              className="text-xs text-red-600 hover:text-red-800"
                            >
                              ✕
                            </button>
                          )}
                        </div>
                        {variant.subject && (
                          <p className="text-xs text-gray-500 mt-0.5 truncate">{variant.subject}</p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          ))}
          {followUps.length === 0 && (
            <p className="text-xs text-gray-500 text-center py-4">
              No follow-ups added yet. Click "+ Add Follow-up" to add one.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

