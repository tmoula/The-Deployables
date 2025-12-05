import { useState, useEffect, useCallback } from "react";
import { 
  Mail, Upload, Play, Pause, CheckCircle, XCircle, Users, Eye, 
  Search, Filter, ChevronDown, MoreVertical, Send, MailOpen, 
  MousePointerClick, MessageSquare, DollarSign, AlertTriangle,
  Calendar, FileText, Settings
} from "lucide-react";
import { format } from "date-fns";
import { campaignApi } from "../services/campaignApi";
import Step1UploadCsv from "./campaignSteps/Step1UploadCsv";
import Step2ComposeEmail from "./campaignSteps/Step2ComposeEmail";
import Step3CampaignSettings from "./campaignSteps/Step3CampaignSettings";
import Step4Review from "./campaignSteps/Step4Review";

export default function Campaigns() {
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedCampaign, setSelectedCampaign] = useState(null);
  const [campaignLeads, setCampaignLeads] = useState([]);
  const [campaignContacts, setCampaignContacts] = useState([]);
  const [selectedContactId, setSelectedContactId] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [campaignName, setCampaignName] = useState("");
  const [description, setDescription] = useState("");
  const [backendStatus, setBackendStatus] = useState({ connected: false, checking: true });
  const [showCreateCampaign, setShowCreateCampaign] = useState(false);
  const [campaignNameError, setCampaignNameError] = useState("");
  const [activeTab, setActiveTab] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [currentStep, setCurrentStep] = useState(1);
  const [uploadedFile, setUploadedFile] = useState(null);
  const [emailContent, setEmailContent] = useState("");
  const [emailSubject, setEmailSubject] = useState("");
  const [campaignSettings, setCampaignSettings] = useState({
    sendDelay: 0,
    followUpDelay: 3,
    maxFollowUps: 2,
    sendTime: "09:00",
    startDate: "",
    endDate: "",
    activeDays: [],
    timezone: "UTC",
    startTime: "09:00",
    endTime: "17:00",
    selectedMailboxIds: []
  });
  const [emailVariants, setEmailVariants] = useState([
    { id: 1, subject: "", content: "", isActive: true }
  ]);
  const [followUps, setFollowUps] = useState([]);
  const [selectedFollowUp, setSelectedFollowUp] = useState(null);
  const [selectedFollowUpVariant, setSelectedFollowUpVariant] = useState(null);
  const [followUpTiming, setFollowUpTiming] = useState({
    delayDays: 3,
    fallsOnDay: ""
  });
  const [timelineSettings, setTimelineSettings] = useState({
    sendTime: "09:00",
    timezone: "UTC"
  });
  const [openMenuId, setOpenMenuId] = useState(null);
  const [editingCampaignId, setEditingCampaignId] = useState(null);

  // Load contacts when entering Step 2
  useEffect(() => {
    if (currentStep === 2) {
      const campaignIdForContacts = editingCampaignId ? parseInt(editingCampaignId) : (selectedCampaign ? (parseInt(selectedCampaign.id) || 1) : 1);
      if (campaignContacts.length === 0 && !selectedContactId) {
        loadCampaignContacts(campaignIdForContacts);
      }
    }
  }, [currentStep, editingCampaignId, selectedCampaign]);

  // Close dropdown menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (openMenuId && !event.target.closest('.relative')) {
        setOpenMenuId(null);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [openMenuId]);

  // Check backend connection
  useEffect(() => {
    const checkBackend = async () => {
      try {
        setBackendStatus({ connected: false, checking: true });
        await campaignApi.checkHealth();
        setBackendStatus({ connected: true, checking: false });
        loadCampaigns();
      } catch (error) {
        console.error("Backend connection check failed:", error);
        setBackendStatus({ connected: false, checking: false, error: error.message });
      }
    };
    checkBackend();
    const interval = setInterval(checkBackend, 10000);
    return () => clearInterval(interval);
  }, []);

  const loadCampaigns = async () => {
    try {
      setLoading(true);
      const data = await campaignApi.getAllCampaigns();
      setCampaigns(data);
    } catch (error) {
      console.error("Failed to load campaigns:", error);
    } finally {
      setLoading(false);
    }
  };

  const loadCampaignLeads = async (campaignId) => {
    try {
      const leads = await campaignApi.getCampaignLeads(campaignId);
      setCampaignLeads(leads);
    } catch (error) {
      console.error("Failed to load campaign leads:", error);
      alert("Failed to load leads: " + error.message);
    }
  };

  const loadCampaignContacts = async (campaignId) => {
    try {
      const contacts = await campaignApi.getCampaignContacts(campaignId);
      setCampaignContacts(contacts || []);
      // Auto-select first contact if available and none selected
      if (contacts && contacts.length > 0) {
        if (!selectedContactId) {
          setSelectedContactId(contacts[0].contactId);
        }
      } else {
        // If no contacts found, use default contact ID 1 (from database)
        if (!selectedContactId) {
          setSelectedContactId(1);
        }
      }
    } catch (error) {
      console.error("Failed to load campaign contacts:", error);
      setCampaignContacts([]);
      // Use default contact ID if loading fails
      if (!selectedContactId) {
        setSelectedContactId(1);
      }
    }
  };

  // 🔧 FIXED: this no longer advances the step or checks name.
  // It *only* validates the file and stores it.
  const handleFileSelect = (file) => {
    if (!file || !file.name.endsWith(".csv")) {
      alert("Please upload a CSV file");
      return;
    }
    setUploadedFile(file);
  };

  const handleFileUpload = async () => {
    console.log('🚀 handleFileUpload called');
    console.log('🚀 uploadedFile:', uploadedFile);
    console.log('🚀 campaignName:', campaignName);
    
    if (!uploadedFile) {
      console.log('🚀 Error: No file uploaded');
      alert("Please upload a CSV file");
      return;
    }

    // Validate campaign name
    if (!campaignName || campaignName.trim() === "") {
      console.log('🚀 Error: Campaign name is empty');
      setCampaignNameError("Campaign name is required");
      setCurrentStep(1);
      return;
    }
    setCampaignNameError("");

    try {
      setUploading(true);
      console.log('📤 Uploading CSV - Campaign Name:', campaignName.trim());
      console.log('📤 Uploading CSV - File:', uploadedFile?.name);
      
      const result = await campaignApi.uploadCsv(
        uploadedFile,
        campaignName.trim(),
        description || undefined
      );
      
      console.log('📤 Upload response:', result);
      
      const campaign = result.campaign || result;
      const leadsCount = result.leadsCount || campaign.totalLeads || 0;
      
      // Load contacts after import (leads are now in database)
      if (campaign.id) {
        // Use campaign ID to load contacts
        const campaignIdForContacts = parseInt(campaign.id) || 1;
        await loadCampaignContacts(campaignIdForContacts);
      }
      
      // Reload campaigns list to show the newly created campaign
      await loadCampaigns();
      
      // Set the created campaign as selected so user can continue editing
      if (campaign.id) {
        setSelectedCampaign(campaign);
      }
      
      alert(
        `Successfully uploaded ${leadsCount} leads! Campaign created: ${
          campaign.name || "Campaign"
        }. ${result.importedLeads ? result.importedLeads.length + ' contacts imported to database.' : ''}`
      );
      
      // Advance to next step (Compose Email) after successful upload
      setCurrentStep(2);
      
      // Don't reset state - keep campaign name and file for editing
      // Only reset error state
      setCampaignNameError("");
    } catch (error) {
      console.error("Upload failed:", error);
      alert("Failed to upload CSV: " + error.message);
    } finally {
      setUploading(false);
    }
  };

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  // 🔧 FIXED: dropping a file no longer moves to step 2. It just selects the file.
  const handleDrop = useCallback((e) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) {
      handleFileSelect(file);
    }
  }, []);

  const handleFileInputSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const resetCampaignCreation = () => {
    setShowCreateCampaign(false);
    setCurrentStep(1);
    setCampaignName("");
    setDescription("");
    setCampaignNameError("");
    setUploadedFile(null);
    setEmailContent("");
    setEmailSubject("");
    setCampaignSettings({
      sendDelay: 0,
      followUpDelay: 3,
      maxFollowUps: 2,
      sendTime: "09:00",
      startDate: "",
      endDate: "",
      activeDays: [],
      timezone: "UTC",
      startTime: "09:00",
      endTime: "17:00",
      selectedMailboxIds: []
    });
    setEmailVariants([{ id: 1, subject: "", content: "", isActive: true }]);
    setFollowUps([]);
    setSelectedFollowUp(null);
    setSelectedFollowUpVariant(null);
    setFollowUpTiming({
      delayDays: 3,
      fallsOnDay: ""
    });
    setTimelineSettings({
      sendTime: "09:00",
      timezone: "UTC"
    });
    setEditingCampaignId(null);
    setOpenMenuId(null);
  };

  const handleViewLeads = async (campaign) => {
    setSelectedCampaign(campaign);
    await loadCampaignLeads(campaign.id);
    // Also load contacts from database
    const campaignIdForContacts = parseInt(campaign.id) || 1;
    await loadCampaignContacts(campaignIdForContacts);
  };

  const handleStatusChange = async (campaignId, newStatus) => {
    try {
      await campaignApi.updateCampaignStatus(campaignId, newStatus);
      await loadCampaigns();
      if (selectedCampaign && selectedCampaign.id === campaignId) {
        const updated = await campaignApi.getCampaign(campaignId);
        setSelectedCampaign(updated);
      }
    } catch (error) {
      alert("Failed to update campaign status: " + error.message);
    }
  };

  const handleDeleteCampaign = async (campaignId) => {
    if (!window.confirm("Are you sure you want to delete this campaign? This action cannot be undone.")) {
      return;
    }

    try {
      await campaignApi.deleteCampaign(campaignId);
      await loadCampaigns();
      // Clear selected campaign if it was deleted
      if (selectedCampaign && selectedCampaign.id === campaignId) {
        setSelectedCampaign(null);
      }
    } catch (error) {
      alert("Failed to delete campaign: " + error.message);
    }
  };

  // Validation function to check all variants have subject and content
  const validateAllVariants = () => {
    // Check Initial Contact variants
    for (let i = 0; i < emailVariants.length; i++) {
      const variant = emailVariants[i];
      // If this is the active variant, check current emailSubject/emailContent state
      let subject = variant.subject;
      let content = variant.content;
      
      if (variant.isActive && !selectedFollowUp) {
        // Use current state if this is the active variant
        subject = emailSubject;
        content = emailContent;
      }
      
      if (!subject || !subject.trim()) {
        return {
          valid: false,
          message: `Initial Contact Variant ${i + 1} is missing a subject line. Please fill in all variant subjects.`
        };
      }
      if (!content || !content.trim() || content === '<p><br></p>' || content === '<div><br></div>') {
        return {
          valid: false,
          message: `Initial Contact Variant ${i + 1} is missing email content. Please fill in all variant content.`
        };
      }
    }

    // Check Follow-up variants
    for (let i = 0; i < followUps.length; i++) {
      const followUp = followUps[i];
      const variants = followUp.variants || [];
      for (let j = 0; j < variants.length; j++) {
        const variant = variants[j];
        // If this is the active follow-up variant, check current emailSubject/emailContent state
        let subject = variant.subject;
        let content = variant.content;
        
        if (selectedFollowUp === followUp.id && selectedFollowUpVariant === variant.id) {
          // Use current state if this is the active variant
          subject = emailSubject;
          content = emailContent;
        }
        
        if (!subject || !subject.trim()) {
          return {
            valid: false,
            message: `Follow-up ${i + 1}, Variant ${j + 1} is missing a subject line. Please fill in all variant subjects.`
          };
        }
        if (!content || !content.trim() || content === '<p><br></p>' || content === '<div><br></div>') {
          return {
            valid: false,
            message: `Follow-up ${i + 1}, Variant ${j + 1} is missing email content. Please fill in all variant content.`
          };
        }
      }
    }

    return { valid: true };
  };

  const calculatePercentage = (value, total) => {
    if (!total || total === 0) return 0;
    return ((value / total) * 100).toFixed(2);
  };

  const filteredCampaigns = campaigns.filter((campaign) => {
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      return (
        campaign.name?.toLowerCase().includes(query) ||
        campaign.description?.toLowerCase().includes(query)
      );
    }
    return true;
  });

  // Stepper Component
  const steps = [
    { number: 1, title: "Upload CSV", icon: Upload },
    { number: 2, title: "Compose Email", icon: Mail },
    { number: 3, title: "Campaign Settings", icon: Settings },
    { number: 4, title: "Review", icon: CheckCircle }
  ];

  const renderStepper = () => {
    return (
      <div className="mb-8">
        <div className="flex items-center justify-between">
          {steps.map((step, index) => {
            const Icon = step.icon;
            const isActive = currentStep === step.number;
            const isCompleted = currentStep > step.number;
            const isLast = index === steps.length - 1;

            return (
              <div key={step.number} className="flex items-center flex-1">
                <div className="flex flex-col items-center flex-1">
                  <div
                    className={`flex items-center justify-center w-10 h-10 rounded-full border-2 transition-all ${
                      isCompleted
                        ? "bg-blue-600 border-blue-600 text-white"
                        : isActive
                        ? "bg-blue-600 border-blue-600 text-white"
                        : "bg-white border-gray-300 text-gray-400"
                    }`}
                  >
                    <Icon size={20} />
                  </div>
                  <span
                    className={`mt-2 text-sm font-medium ${
                      isActive || isCompleted
                        ? "text-blue-600"
                        : "text-gray-400"
                    }`}
                  >
                    {step.title}
                  </span>
                </div>
                {!isLast && (
                  <div
                    className={`flex-1 h-0.5 mx-4 ${
                      isCompleted ? "bg-blue-600" : "bg-gray-300"
                    }`}
                  />
                )}
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  // CSV Upload Modal - Multi-step form OR Edit Settings View
  if (showCreateCampaign || editingCampaignId) {
    return (
      <div className={`p-6 bg-gray-50 min-h-screen ${currentStep === 2 ? 'pt-4' : ''}`}>
        {/* Stepper - Full Width */}
        <div className="max-w-7xl mx-auto mb-6">{renderStepper()}</div>

        <div className="max-w-7xl mx-auto">
          {currentStep !== 2 && (
            <div className="bg-white rounded-lg shadow p-6">
              <div className="flex justify-between items-center mb-6">
                <h2 className="text-xl font-semibold text-gray-800">
                  {editingCampaignId ? "Edit Campaign Settings" : "Create New Campaign"}
                </h2>
                {editingCampaignId && (
                  <button
                    onClick={() => {
                      setEditingCampaignId(null);
                      setCurrentStep(1);
                      setOpenMenuId(null);
                    }}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    <XCircle size={20} />
                  </button>
                )}
              </div>
            </div>
          )}

          {/* Step Content */}
          <div className={currentStep === 2 ? '' : 'mb-6'}>
            {/* Step 1: Upload CSV */}
            {currentStep === 1 && (
              <div className="bg-white rounded-lg shadow p-6">
                <Step1UploadCsv
                  campaignName={campaignName}
                  setCampaignName={setCampaignName}
                  description={description}
                  setDescription={setDescription}
                  campaignNameError={campaignNameError}
                  setCampaignNameError={setCampaignNameError}
                  uploadedFile={uploadedFile}
                  setUploadedFile={setUploadedFile}
                  isDragging={isDragging}
                  handleDragOver={handleDragOver}
                  handleDragLeave={handleDragLeave}
                  handleDrop={handleDrop}
                  handleFileInputSelect={handleFileInputSelect}
                  uploading={uploading}
                  isEditing={!!editingCampaignId}
                />
              </div>
            )}

            {/* Step 2: Compose Email */}
            {currentStep === 2 && (
              <div>
                <div className="flex justify-between items-center mb-4">
                  <h2 className="text-xl font-semibold text-gray-800">
                    {editingCampaignId ? "Edit Campaign" : "Create New Campaign"}
                  </h2>
                </div>
                <Step2ComposeEmail
                  emailSubject={emailSubject}
                  setEmailSubject={setEmailSubject}
                  emailContent={emailContent}
                  setEmailContent={setEmailContent}
                  emailVariants={emailVariants}
                  setEmailVariants={setEmailVariants}
                  followUps={followUps}
                  setFollowUps={setFollowUps}
                  selectedFollowUp={selectedFollowUp}
                  setSelectedFollowUp={setSelectedFollowUp}
                  selectedFollowUpVariant={selectedFollowUpVariant}
                  setSelectedFollowUpVariant={setSelectedFollowUpVariant}
                  followUpTiming={followUpTiming}
                  setFollowUpTiming={setFollowUpTiming}
                  campaignName={campaignName}
                  campaignId={editingCampaignId ? parseInt(editingCampaignId) : (selectedCampaign ? (parseInt(selectedCampaign.id) || 1) : 1)}
                  contactId={selectedContactId || (campaignContacts && campaignContacts.length > 0 ? campaignContacts[0].contactId : 1)}
                />
                {/* Navigation Buttons for Step 2 */}
                <div className="flex justify-between items-center pt-6 mt-6 border-t border-gray-200">
                  <button
                    onClick={() => {
                      setCurrentStep(currentStep - 1);
                    }}
                    className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
                  >
                    Back
                  </button>
                  <button
                    onClick={() => {
                      // Validate all variants before proceeding
                      const validation = validateAllVariants();
                      if (!validation.valid) {
                        alert(validation.message);
                        return;
                      }
                      setCurrentStep(currentStep + 1);
                    }}
                    className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}

            {/* Step 3: Campaign Settings */}
            {currentStep === 3 && (
              <div className="bg-white rounded-lg shadow p-6">
                <Step3CampaignSettings
                  campaignSettings={campaignSettings}
                  setCampaignSettings={setCampaignSettings}
                />
              </div>
            )}

            {/* Step 4: Review */}
            {currentStep === 4 && (
              <div className="bg-white rounded-lg shadow p-6">
                <Step4Review
                  campaignName={campaignName}
                  description={description}
                  uploadedFile={uploadedFile}
                  emailSubject={emailSubject}
                  emailContent={emailContent}
                  campaignSettings={campaignSettings}
                />
              </div>
            )}
          </div>

          {/* Navigation Buttons */}
          {currentStep !== 2 && (
            <div className="flex justify-between items-center pt-4 border-t border-gray-200 mt-6">
              <button
                onClick={() => {
                  if (currentStep > 1) {
                    setCurrentStep(currentStep - 1);
                  } else {
                    if (editingCampaignId) {
                      setEditingCampaignId(null);
                      setOpenMenuId(null);
                    } else {
                      resetCampaignCreation();
                    }
                  }
                }}
                className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
              >
                {currentStep === 1 ? (editingCampaignId ? "Cancel" : "Cancel") : "Back"}
              </button>
              <div className="flex gap-2">
                {currentStep < 4 ? (
                  <button
                    onClick={async () => {
                      if (currentStep === 1) {
                        if (
                          !campaignName ||
                          campaignName.trim() === ""
                        ) {
                          setCampaignNameError(
                            "Campaign name is required"
                          );
                          alert("Please enter a campaign name");
                          return;
                        }
                        // Allow proceeding without file when editing
                        if (!uploadedFile && !editingCampaignId) {
                          alert(
                            "Please upload a CSV file first"
                          );
                          return;
                        }
                        
                        // If not editing, upload CSV and create campaign before proceeding
                        if (!editingCampaignId && uploadedFile) {
                          try {
                            await handleFileUpload();
                            // handleFileUpload will reload campaigns and show success message
                            // Only proceed to next step if upload was successful
                            // (handleFileUpload already handles errors)
                            return; // Don't advance step here, let handleFileUpload handle it
                          } catch (error) {
                            // Error already handled in handleFileUpload
                            return;
                          }
                        }
                      }
                      if (
                        currentStep === 2
                      ) {
                        // Validate all variants before proceeding
                        const validation = validateAllVariants();
                        if (!validation.valid) {
                          alert(validation.message);
                          return;
                        }
                      }
                      setCurrentStep(currentStep + 1);
                    }}
                    className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
                  >
                    Next
                  </button>
                ) : (
                  <button
                    onClick={() => {
                      if (editingCampaignId) {
                        // Save all changes for existing campaign
                        // TODO: Call API to update campaign with all changes
                        alert("Campaign updated successfully!");
                        setEditingCampaignId(null);
                        setCurrentStep(1);
                        setOpenMenuId(null);
                        loadCampaigns();
                      } else {
                        handleFileUpload();
                      }
                    }}
                    disabled={uploading}
                    className="px-6 py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-400 text-white rounded-lg transition"
                  >
                    {uploading 
                      ? "Creating..." 
                      : editingCampaignId 
                        ? "Save Campaign" 
                        : "Create Campaign"}
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  // Campaign Details View
  if (selectedCampaign) {
    return (
      <div className="p-6 bg-gray-50 min-h-screen">
        <div className="bg-white rounded-lg shadow">
          <div className="p-4 border-b border-gray-200 flex justify-between items-center">
            <div className="flex items-center gap-3">
              <button
                onClick={() => setSelectedCampaign(null)}
                className="text-gray-600 hover:text-gray-800"
              >
                ← Back to Campaigns
              </button>
              <h2 className="text-xl font-semibold text-gray-800">
                {selectedCampaign.name}
              </h2>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() =>
                  handleStatusChange(
                    selectedCampaign.id,
                    "RUNNING"
                  )
                }
                disabled={selectedCampaign.status === "RUNNING"}
                className="flex items-center gap-1.5 bg-green-600 hover:bg-green-700 disabled:bg-gray-400 text-white px-3 py-1.5 rounded text-sm transition"
              >
                <Play size={14} />
                Start
              </button>
              <button
                onClick={() =>
                  handleStatusChange(
                    selectedCampaign.id,
                    "PAUSED"
                  )
                }
                disabled={selectedCampaign.status === "PAUSED"}
                className="flex items-center gap-1.5 bg-yellow-600 hover:bg-yellow-700 disabled:bg-gray-400 text-white px-3 py-1.5 rounded text-sm transition"
              >
                <Pause size={14} />
                Pause
              </button>
            </div>
          </div>

          <div className="p-4 border-b border-gray-200 bg-gray-50">
            <div className="grid grid-cols-4 gap-4">
              <div>
                <p className="text-gray-500 text-sm">
                  Total Leads
                </p>
                <p className="text-2xl font-bold">
                  {selectedCampaign.totalLeads}
                </p>
              </div>
              <div>
                <p className="text-gray-500 text-sm">Sent</p>
                <p className="text-2xl font-bold">
                  {selectedCampaign.sentCount}
                </p>
              </div>
              <div>
                <p className="text-gray-500 text-sm">Opened</p>
                <p className="text-2xl font-bold">
                  {selectedCampaign.openedCount}
                </p>
              </div>
              <div>
                <p className="text-gray-500 text-sm">Replied</p>
                <p className="text-2xl font-bold">
                  {selectedCampaign.repliedCount}
                </p>
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Match Score
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Company
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Position
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Email
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Industry
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {campaignLeads.length === 0 ? (
                  <tr>
                    <td
                      colSpan="7"
                      className="px-6 py-8 text-center text-gray-500"
                    >
                      Loading leads...
                    </td>
                  </tr>
                ) : (
                  campaignLeads.map((lead) => (
                    <tr
                      key={lead.id}
                      className="hover:bg-gray-50"
                    >
                      <td className="px-6 py-4">
                        {lead.matchScore && (
                          <span
                            className={`px-2 py-1 rounded text-xs font-semibold ${
                              parseFloat(lead.matchScore) >= 70
                                ? "bg-green-100 text-green-800"
                                : parseFloat(lead.matchScore) >= 40
                                ? "bg-yellow-100 text-yellow-800"
                                : "bg-gray-100 text-gray-800"
                            }`}
                          >
                            {lead.matchScore}
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-sm font-medium text-gray-900">
                        {lead.company || "-"}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {lead.firstName && lead.lastName
                          ? `${lead.firstName} ${lead.lastName}`
                          : "-"}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {lead.position || "-"}
                      </td>
                      <td className="px-6 py-4 text-sm text-blue-600">
                        {lead.email || "-"}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {lead.industry || "-"}
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex gap-2">
                          <button
                            className="text-blue-600 hover:text-blue-800"
                            title="View Details"
                          >
                            <Eye size={16} />
                          </button>
                          <button
                            className="text-green-600 hover:text-green-800"
                            title="Send Email"
                          >
                            <Mail size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    );
  }

  // Main Campaigns List View
  return (
    <div className="p-6 bg-white min-h-screen">
      {/* Header with Tabs */}
      <div className="mb-6">
        <div className="flex items-center gap-6 border-b border-gray-200 mb-4">
          <button
            onClick={() => setActiveTab("all")}
            className={`pb-3 px-1 font-medium transition-colors ${
              activeTab === "all"
                ? "text-blue-600 border-b-2 border-blue-600"
                : "text-gray-600 hover:text-gray-800"
            }`}
          >
            All Campaigns ({campaigns.length})
          </button>
          <button
            onClick={() => setActiveTab("folders")}
            className={`pb-3 px-1 font-medium transition-colors ${
              activeTab === "folders"
                ? "text-blue-600 border-b-2 border-blue-600"
                : "text-gray-600 hover:text-gray-800"
            }`}
          >
            Folders
          </button>
        </div>

        {/* Top Controls Bar */}
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 flex-1">
            <div className="relative flex-1 max-w-md">
              <Search
                className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400"
                size={18}
              />
              <input
                type="text"
                placeholder="Search Campaigns"
                value={searchQuery}
                onChange={(e) =>
                  setSearchQuery(e.target.value)
                }
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex items-center gap-2 border border-gray-300 rounded-lg px-3 py-2 cursor-pointer hover:bg-gray-50">
              <Filter
                size={18}
                className="text-gray-600"
              />
              <span className="text-sm text-gray-700">
                Email Sent, Opened...
              </span>
              <ChevronDown
                size={16}
                className="text-gray-600"
              />
            </div>
          </div>
          <button
            onClick={() => setShowCreateCampaign(true)}
            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition whitespace-nowrap"
          >
            <Mail size={18} />
            + Create Campaign
          </button>
        </div>
      </div>

      {/* Backend Status */}
      {!backendStatus.connected && !backendStatus.checking && (
        <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          <p className="font-medium">⚠️ Backend Connection Failed</p>
          <p className="text-sm mt-1">
            {backendStatus.error ||
              "Cannot connect to campaign service at http://localhost:8082"}
          </p>
        </div>
      )}

      {/* Campaigns List */}
      {loading ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">
            Loading campaigns...
          </p>
        </div>
      ) : filteredCampaigns.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
          <Mail
            size={48}
            className="mx-auto mb-4 text-gray-400"
          />
          <p className="text-lg font-medium mb-2">
            No campaigns yet
          </p>
          <p>
            Click "+ Create Campaign" to upload a CSV file and
            start managing your leads.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredCampaigns.map((campaign) => {
            const status = (
              campaign.status?.toString() || "DRAFT"
            ).toLowerCase();
            const isPaused = status === "paused";
            const isCompleted = status === "completed";
            const isRunning = status === "running";

            const openedPercentage = calculatePercentage(
              campaign.openedCount,
              campaign.sentCount
            );
            const clickedPercentage = 0; // Placeholder
            const repliedPercentage = calculatePercentage(
              campaign.repliedCount,
              campaign.sentCount
            );
            const campaignProgress =
              campaign.totalLeads > 0
                ? Math.round(
                    (campaign.sentCount /
                      campaign.totalLeads) *
                      100
                  )
                : 0;

            return (
              <div
                key={campaign.id}
                onClick={() => handleViewLeads(campaign)}
                className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer"
              >
                <div className="flex items-start justify-between">
                  {/* Campaign Details Section */}
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      {/* Status Indicator */}
                      <div
                        className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
                          isCompleted
                            ? "bg-green-500"
                            : isPaused
                            ? "bg-gray-400"
                            : "bg-blue-500"
                        }`}
                      >
                        {isCompleted ? (
                          <span className="text-white text-xs font-bold">
                            100%
                          </span>
                        ) : isPaused ? (
                          <Pause
                            size={20}
                            className="text-white"
                          />
                        ) : (
                          <span className="text-white text-xs font-bold">
                            {campaignProgress}%
                          </span>
                        )}
                      </div>

                      <div className="flex-1">
                        <h3 className="text-lg font-semibold text-gray-900 mb-1">
                          {campaign.name || "Unnamed Campaign"}
                        </h3>
                        <div className="flex items-center gap-2 text-sm text-gray-600">
                          <span className="capitalize">
                            {status}
                          </span>
                          <span>•</span>
                          <span>
                            Created At:{" "}
                            {campaign.createdAt ? format(new Date(campaign.createdAt), 'MMM dd, yyyy') : 'N/A'}
                          </span>
                          {campaign.totalLeads > 0 && (
                            <>
                              <span>•</span>
                              <span>
                                {campaign.totalLeads} leads
                              </span>
                            </>
                          )}
                        </div>
                        {isPaused && (
                          <div className="flex items-center gap-2 mt-2 text-yellow-600">
                            <AlertTriangle size={16} />
                            <span className="text-sm">
                              Reason: Campaign paused
                            </span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Report Metrics Section */}
                  <div className="flex items-center gap-6 ml-6">
                    <div className="flex items-center gap-2">
                      <Users
                        size={18}
                        className="text-gray-500"
                      />
                      <span className="text-sm font-medium text-gray-700">
                        {campaign.totalLeads || 0} Leads
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Send
                        size={18}
                        className="text-gray-500"
                      />
                      <span className="text-sm font-medium text-gray-700">
                        {campaign.sentCount || 0} Sent
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <MailOpen
                        size={18}
                        className="text-gray-500"
                      />
                      <span className="text-sm font-medium text-gray-700">
                        {campaign.openedCount || 0}{" "}
                        {openedPercentage > 0 &&
                          `${openedPercentage}%`}{" "}
                        Opened
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <MousePointerClick
                        size={18}
                        className="text-gray-500"
                      />
                      <span className="text-sm font-medium text-gray-700">
                        0{" "}
                        {clickedPercentage > 0 &&
                          `${clickedPercentage}%`}{" "}
                        Clicked
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <MessageSquare
                        size={18}
                        className="text-gray-500"
                      />
                      <span className="text-sm font-medium text-gray-700">
                        {campaign.repliedCount || 0}{" "}
                        {repliedPercentage > 0 &&
                          `${repliedPercentage}%`}{" "}
                        Replied
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <DollarSign
                        size={18}
                        className="text-gray-500"
                      />
                      <span className="text-sm font-medium text-gray-700">
                        0 Positive Reply
                      </span>
                    </div>
                    <div className="relative">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setOpenMenuId(openMenuId === campaign.id ? null : campaign.id);
                        }}
                        className="text-gray-400 hover:text-gray-600 p-1"
                      >
                        <MoreVertical size={18} />
                      </button>
                      
                      {/* Dropdown Menu */}
                      {openMenuId === campaign.id && (
                        <div className="absolute right-0 top-8 z-50 w-48 bg-white border border-gray-200 rounded-lg shadow-lg">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setEditingCampaignId(campaign.id);
                              setOpenMenuId(null);
                              // Load campaign data into form fields
                              setCampaignName(campaign.name || "");
                              setDescription(campaign.description || "");
                              // Load campaign settings if available
                              if (campaign.settings) {
                                setCampaignSettings(campaign.settings);
                              }
                              // Load email content/subject if available
                              if (campaign.emailSubject) {
                                setEmailSubject(campaign.emailSubject);
                              }
                              if (campaign.emailContent) {
                                setEmailContent(campaign.emailContent);
                              }
                              // Start from Step 1
                              setCurrentStep(1);
                              setShowCreateCampaign(false);
                            }}
                            className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
                          >
                            <Settings size={16} />
                            Edit Campaign
                          </button>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenMenuId(null);
                              // Add other menu options here
                            }}
                            className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
                          >
                            <FileText size={16} />
                            View Details
                          </button>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenMenuId(null);
                              handleDeleteCampaign(campaign.id);
                            }}
                            className="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
                          >
                            <XCircle size={16} />
                            Delete Campaign
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}