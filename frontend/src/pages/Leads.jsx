import { useState, useEffect } from "react";
import { Sparkles, CheckCircle, XCircle, ChevronDown, ChevronUp, Building2, Target, DollarSign, Code, Users, MapPin, TrendingUp, Info, Download } from "lucide-react";
import { api } from "../services/api";

export default function Leads() {
  const [loading, setLoading] = useState(false);
  const [matchedProspects, setMatchedProspects] = useState([]);
  const [errors, setErrors] = useState({});
  const [criteriaErrors, setCriteriaErrors] = useState({});
  const [hasSearched, setHasSearched] = useState(false);
  const [backendStatus, setBackendStatus] = useState({ connected: false, checking: true });
  // Removed savingSeller, savingCriteria, saveMessage - no longer needed
  
  // Collapsible sections state - All expanded by default
  const [expandedSellerSections, setExpandedSellerSections] = useState({
    basics: true,
    product: true,
    tech: true,
    gtm: true
  });
  const [expandedCriteriaSections, setExpandedCriteriaSections] = useState({
    general: true,
    financials: true,
    technology: true,
    decisionMakers: true,
    geography: true,
    behavioral: true
  });

  // Check backend connection on component mount
  useEffect(() => {
    const checkBackend = async () => {
      try {
        setBackendStatus({ connected: false, checking: true });
        await api.checkHealth();
        setBackendStatus({ connected: true, checking: false });
      } catch (error) {
        console.error("Backend connection check failed:", error);
        setBackendStatus({ connected: false, checking: false, error: error.message });
      }
    };
    checkBackend();
    const interval = setInterval(checkBackend, 10000);
    return () => clearInterval(interval);
  }, []);

  // Load saved data from localStorage on component mount
  useEffect(() => {
    const savedSellerProfile = localStorage.getItem('sellerProfile');
    if (savedSellerProfile) {
      try {
        const parsed = JSON.parse(savedSellerProfile);
        setSellerProfile(parsed);
      } catch (error) {
        console.error("Error loading saved seller profile:", error);
      }
    }

    const savedProspectCriteria = localStorage.getItem('prospectCriteria');
    if (savedProspectCriteria) {
      try {
        const parsed = JSON.parse(savedProspectCriteria);
        setProspectCriteria(parsed);
      } catch (error) {
        console.error("Error loading saved prospect criteria:", error);
      }
    }
  }, []);

  // Debug loading state
  useEffect(() => {
    console.log("Loading state changed:", loading);
  }, [loading]);

  // Seller Profile State - Expanded with all new fields
  const [sellerProfile, setSellerProfile] = useState({
    // Company Basics
    companyName: "",
    industry: "",
    companySize: "",
    foundedYear: "",
    headquartersRegion: "",
    // Product/Offering
    valuePropositionKeywords: "",
    targetCustomerSegment: "",
    priceTier: "",
    // Technology Stack
    techStack: "",
    // Go-to-Market
    salesModel: "",
    targetRegions: "",
    // Legacy/Description
    description: ""
  });

  // Prospect Criteria State - Expanded with all new fields
  const [prospectCriteria, setProspectCriteria] = useState({
    // General
    companyName: "",
    domain: "",
    industry: "",
    minSize: "",
    maxSize: "",
    minFoundedYear: "",
    maxFoundedYear: "",
    // Financials
    fundingStage: "",
    annualRevenueRange: "",
    growthRate: "",
    // Technology
    techUsed: "",
    techCategory: "",
    // Decision Makers
    targetRoles: "",
    seniorityLevel: "",
    // Geography
    headquartersRegion: "",
    hqCountry: "",
    remoteFriendly: "",
    regions: "",
    // Behavioral/Intent
    hiringTrends: "",
    recentTechAdoption: "",
    keywordMentions: "",
    // Legacy fields
    requiredStack: "",
    desiredKeywords: ""
  });

  const toggleSellerSection = (section) => {
    setExpandedSellerSections(prev => ({ ...prev, [section]: !prev[section] }));
  };

  const toggleCriteriaSection = (section) => {
    setExpandedCriteriaSections(prev => ({ ...prev, [section]: !prev[section] }));
  };

  const handleSellerChange = (e) => {
    setSellerProfile({ ...sellerProfile, [e.target.name]: e.target.value });
    if (errors[e.target.name]) {
      setErrors({ ...errors, [e.target.name]: "" });
    }
  };

  const handleCriteriaChange = (e) => {
    setProspectCriteria({ ...prospectCriteria, [e.target.name]: e.target.value });
    if (criteriaErrors[e.target.name]) {
      setCriteriaErrors({ ...criteriaErrors, [e.target.name]: "" });
    }
  };

  const fillExampleData = () => {
    // Navoy Company Profile
    setSellerProfile({
      companyName: "Navoy",
      industry: "Travel tech",
      companySize: "5",
      foundedYear: "2024",
      headquartersRegion: "US",
      valuePropositionKeywords: "Travel Planner engine, AI, automation",
      targetCustomerSegment: "Enterprises",
      priceTier: "",
      techStack: "",
      salesModel: "",
      targetRegions: "US, EU",
      description: "We provide a AI engine to generate personalized trips for the travelers."
    });

    // Navoy Prospect Search Criteria
    setProspectCriteria({
      companyName: "",
      domain: "",
      industry: "Travel",
      minSize: "10",
      maxSize: "600",
      minFoundedYear: "",
      maxFoundedYear: "",
      fundingStage: "",
      annualRevenueRange: "",
      growthRate: "",
      techUsed: "",
      techCategory: "",
      targetRoles: "CEO",
      seniorityLevel: "Manager",
      headquartersRegion: "",
      hqCountry: "",
      remoteFriendly: "",
      regions: "US, EU",
      hiringTrends: "",
      recentTechAdoption: "",
      keywordMentions: "",
      requiredStack: "",
      desiredKeywords: ""
    });

    setErrors({});
    setCriteriaErrors({});
  };

  const validateSellerProfile = () => {
    const newErrors = {};
    if (!sellerProfile.companyName?.trim()) newErrors.companyName = "Company name is required";
    if (!sellerProfile.industry?.trim()) newErrors.industry = "Industry is required";
    if (!sellerProfile.companySize?.trim()) newErrors.companySize = "Company size is required";
    if (!sellerProfile.valuePropositionKeywords?.trim()) newErrors.valuePropositionKeywords = "Value proposition keywords are required";
    if (!sellerProfile.targetRegions?.trim()) newErrors.targetRegions = "Target regions are required";
    if (!sellerProfile.description?.trim()) newErrors.description = "Company description is required";
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const validateProspectCriteria = () => {
    const newErrors = {};
    if (!prospectCriteria.industry?.trim()) newErrors.industry = "Target industry is required";
    if (!prospectCriteria.minSize?.trim()) newErrors.minSize = "Minimum company size is required";
    if (!prospectCriteria.maxSize?.trim()) newErrors.maxSize = "Maximum company size is required";
    if (!prospectCriteria.regions?.trim() && !prospectCriteria.headquartersRegion?.trim()) {
      newErrors.regions = "At least one geographic filter is required";
    }
    
    setCriteriaErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Save buttons removed - data is saved automatically when generating leads

  const handleGenerateLeads = async () => {
    if (!validateSellerProfile()) {
      alert("Please fill in all required company information fields.");
      return;
    }
    
    if (!validateProspectCriteria()) {
      alert("Please fill in all required prospect criteria fields.");
      return;
    }

    try {
      console.log("Setting loading to true...");
      setLoading(true);
      // Force a re-render to show loading immediately
      await new Promise(resolve => setTimeout(resolve, 100));
      
      try {
        await api.checkHealth();
      } catch (healthError) {
        setLoading(false);
        throw new Error("Cannot connect to backend. Make sure it's running on http://localhost:8081");
      }
      
      // Build seller data with new fields
      const sellerData = {
        companyName: sellerProfile.companyName.trim(),
        industry: sellerProfile.industry.trim(),
        companySize: parseInt(sellerProfile.companySize),
        foundedYear: sellerProfile.foundedYear ? parseInt(sellerProfile.foundedYear) : null,
        headquartersRegion: sellerProfile.headquartersRegion.trim() || null,
        valuePropositionKeywords: sellerProfile.valuePropositionKeywords
          .split(",")
          .map(s => s.trim())
          .filter(Boolean),
        targetCustomerSegment: sellerProfile.targetCustomerSegment.trim() || null,
        priceTier: sellerProfile.priceTier.trim() || null,
        techStack: sellerProfile.techStack
          .split(",")
          .map(s => s.trim())
          .filter(Boolean),
        salesModel: sellerProfile.salesModel.trim() || null,
        targetRegions: sellerProfile.targetRegions
          .split(",")
          .map(s => s.trim())
          .filter(Boolean)
      };

      await api.setSeller(sellerData);

      // Build criteria with new fields
      const criteria = {
        companyName: prospectCriteria.companyName.trim() || null,
        domain: prospectCriteria.domain.trim() || null,
        industry: prospectCriteria.industry.trim(),
        minSize: parseInt(prospectCriteria.minSize),
        maxSize: parseInt(prospectCriteria.maxSize),
        minFoundedYear: prospectCriteria.minFoundedYear ? parseInt(prospectCriteria.minFoundedYear) : null,
        maxFoundedYear: prospectCriteria.maxFoundedYear ? parseInt(prospectCriteria.maxFoundedYear) : null,
        fundingStage: prospectCriteria.fundingStage.trim() || null,
        annualRevenueRange: prospectCriteria.annualRevenueRange.trim() || null,
        growthRate: prospectCriteria.growthRate.trim() || null,
        techUsed: prospectCriteria.techUsed
          ? prospectCriteria.techUsed.split(",").map(s => s.trim()).filter(Boolean)
          : null,
        techCategory: prospectCriteria.techCategory.trim() || null,
        targetRoles: prospectCriteria.targetRoles
          ? prospectCriteria.targetRoles.split(",").map(s => s.trim()).filter(Boolean)
          : null,
        seniorityLevel: prospectCriteria.seniorityLevel.trim() || null,
        headquartersRegion: prospectCriteria.headquartersRegion.trim() || null,
        hqCountry: prospectCriteria.hqCountry.trim() || null,
        remoteFriendly: prospectCriteria.remoteFriendly ? prospectCriteria.remoteFriendly === "true" : null,
        regions: prospectCriteria.regions
          ? prospectCriteria.regions.split(",").map(s => s.trim()).filter(Boolean)
          : [],
        hiringTrends: prospectCriteria.hiringTrends.trim() || null,
        recentTechAdoption: prospectCriteria.recentTechAdoption.trim() || null,
        keywordMentions: prospectCriteria.keywordMentions
          ? prospectCriteria.keywordMentions.split(",").map(s => s.trim()).filter(Boolean)
          : null,
        // Legacy fields for backward compatibility
        requiredStack: prospectCriteria.techUsed
          ? prospectCriteria.techUsed.split(",").map(s => s.trim()).filter(Boolean)
          : [],
        desiredKeywords: prospectCriteria.keywordMentions
          ? prospectCriteria.keywordMentions.split(",").map(s => s.trim()).filter(Boolean)
          : []
      };

      // Start lead generation - returns batch_id
      const batchResponse = await api.matchProspects(criteria, 5);
      console.log("Batch response:", batchResponse);
      console.log("Batch response type:", typeof batchResponse);
      console.log("Is array?", Array.isArray(batchResponse));
      
      // Handle both old format (array) and new format (object with batchId)
      if (Array.isArray(batchResponse)) {
        // Old format - backend hasn't been updated yet
        console.warn("Backend returned old format (array). Please rebuild and restart lead-svc.");
        setMatchedProspects(batchResponse.map(item => ({
          prospect: item.prospect || item,
          score: item.score || 100
        })));
        setHasSearched(true);
        setLoading(false);
        return;
      }
      
      if (!batchResponse || !batchResponse.batchId) {
        throw new Error("No batch ID returned from server. Backend may need to be rebuilt.");
      }
      
      // Poll for batch status
      const pollBatchStatus = async (batchId) => {
        const maxAttempts = 30; // 30 attempts = 60 seconds max
        let attempts = 0;
        
        while (attempts < maxAttempts) {
          await new Promise(resolve => setTimeout(resolve, 2000)); // Wait 2 seconds
          
          try {
            const status = await api.getBatchStatus(batchId);
            console.log(`Batch ${batchId} status:`, status.status);
            
            if (status.status === 'ready') {
              // Fetch leads
              const leads = await api.getBatchLeads(batchId);
              console.log("Fetched leads:", leads);
              
              // Convert leads to ScoredProspect format for compatibility
              const scoredProspects = leads.map(lead => ({
                prospect: {
                  id: lead.id?.toString() || '',
                  company: lead.companyName || '',
                  firstName: lead.firstName || '',
                  lastName: lead.lastName || '',
                  position: lead.jobTitle || '',
                  email: lead.email || '',
                  domain: lead.companyWebsite || '',
                  industry: null,
                  size: null,
                  regions: [],
                  stack: [],
                  keywords: [],
                  personalizationHook: null
                },
                score: 100 // Default score since we don't calculate it anymore
              }));
              
              setMatchedProspects(scoredProspects);
              setHasSearched(true);
              setLoading(false);
              return;
            } else if (status.status === 'failed') {
              throw new Error(status.errorMessage || 'Lead generation failed');
            }
            
            attempts++;
          } catch (error) {
            console.error("Error polling batch status:", error);
            if (attempts >= maxAttempts - 1) {
              throw error;
            }
          }
        }
        
        throw new Error("Timeout waiting for lead generation to complete");
      };
      
      await pollBatchStatus(batchResponse.batchId);
      // Leads are already set inside pollBatchStatus
      setHasSearched(true);
    } catch (error) {
      console.error("Error generating leads:", error);
      alert(`Failed to generate leads: ${error.message}\n\nMake sure backend is running on http://localhost:8081`);
      setMatchedProspects([]);
      setHasSearched(true);
    } finally {
      setLoading(false);
    }
  };

  const downloadCSV = () => {
    if (matchedProspects.length === 0) return;

    // CSV Headers
    const headers = [
      "Match Score",
      "Company",
      "First Name",
      "Last Name",
      "Position",
      "Email",
      "Domain",
      "Industry",
      "Company Size",
      "Regions",
      "Tech Stack",
      "Keywords",
      "Notes",
      "Personalization Hook"
    ];

    // Convert prospects to CSV rows
    const csvRows = matchedProspects.map((item) => {
      // Handle both ScoredProspect structure {prospect: {...}, score: ...} and direct Prospect
      const prospect = item.prospect || item;
      const score = item.score !== undefined ? item.score : 0;
      
      // Debug logging for first row
      if (matchedProspects.indexOf(item) === 0) {
        console.log("CSV - Processing first prospect:", prospect);
        console.log("CSV - firstName:", prospect.firstName);
        console.log("CSV - lastName:", prospect.lastName);
        console.log("CSV - position:", prospect.position);
        console.log("CSV - email:", prospect.email);
      }
      
      return [
        score.toFixed(1),
        prospect.company || "",
        prospect.firstName || "",
        prospect.lastName || "",
        prospect.position || "",
        prospect.email || "",
        prospect.domain || "",
        prospect.industry || "",
        prospect.size || "",
        prospect.regions && prospect.regions.length > 0 ? prospect.regions.join("; ") : "",
        prospect.stack && prospect.stack.length > 0 ? prospect.stack.join("; ") : "",
        prospect.keywords && prospect.keywords.length > 0 ? prospect.keywords.join("; ") : "",
        "", // Notes column - will be populated by AI service later
        prospect.personalizationHook || "" // Personalization Hook column - AI-generated hook for cold outreach emails
      ];
    });

    // Escape CSV values (handle commas, quotes, newlines)
    const escapeCSV = (value) => {
      if (value === null || value === undefined) return "";
      const stringValue = String(value);
      if (stringValue.includes(",") || stringValue.includes('"') || stringValue.includes("\n")) {
        return `"${stringValue.replace(/"/g, '""')}"`;
      }
      return stringValue;
    };

    // Build CSV content
    const csvContent = [
      headers.map(escapeCSV).join(","),
      ...csvRows.map(row => row.map(escapeCSV).join(","))
    ].join("\n");

    // Create blob and download
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    
    link.setAttribute("href", url);
    link.setAttribute("download", `matched_prospects_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const SectionHeader = ({ icon: Icon, title, expanded, onToggle, children }) => (
    <div 
      className="flex items-center justify-between p-4 bg-gray-50 hover:bg-gray-100 cursor-pointer rounded-t-lg border-b border-gray-200 transition"
      onClick={onToggle}
    >
      <div className="flex items-center gap-3">
        {Icon && <Icon size={20} className="text-blue-600" />}
        <h3 className="font-semibold text-gray-800">{title}</h3>
        {children && <span className="text-xs text-gray-500">({children})</span>}
      </div>
      {expanded ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
    </div>
  );

  return (
    <>
      {/* Single Popup Loader */}
      {loading && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" style={{ left: '256px' }}>
          <div className="bg-transparent rounded-xl p-8">
            <div className="flex flex-col items-center justify-center">
              {/* Rotating Gemini Logo with Gradient */}
              <div className="mb-6">
                <div className="relative inline-block">
                  <div className="absolute inset-0 bg-gradient-to-r from-blue-600 to-purple-600 rounded-full blur-sm opacity-70"></div>
                  <Sparkles 
                    size={64} 
                    className="animate-spin relative" 
                    style={{ 
                      color: 'rgb(37, 99, 235)',
                      filter: 'drop-shadow(0 0 3px rgb(147, 51, 234))'
                    }} 
                  />
                </div>
              </div>
              {/* Single Progress Line */}
              <div className="w-48 h-1 bg-gray-300/30 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-blue-600 to-purple-600 rounded-full"
                  style={{ 
                    width: '100%',
                    animation: 'progressFill 22s ease-in-out'
                  }}
                ></div>
              </div>
            </div>
          </div>
        </div>
      )}
    <div className={`p-6 bg-gray-50 min-h-screen transition-all duration-300 ${loading ? 'blur-sm' : ''}`} data-tour="leads-page">
      <div className="mb-6">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-3xl font-bold text-gray-800">Lead Generation</h1>
            <p className="text-gray-600 mt-2">Enter your company info and prospect criteria to find matching leads</p>
          </div>
          <div className="flex items-center gap-2">
            {backendStatus.checking ? (
              <div className="flex items-center gap-2 text-gray-500">
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-500"></div>
                <span className="text-sm">Checking backend...</span>
              </div>
            ) : backendStatus.connected ? (
              <div className="flex items-center gap-2 text-green-600">
                <CheckCircle size={20} />
                <span className="text-sm font-medium">Backend Connected</span>
              </div>
            ) : (
              <div className="flex items-center gap-2 text-red-600">
                <XCircle size={20} />
                <span className="text-sm font-medium">Backend Offline</span>
              </div>
            )}
          </div>
        </div>
        {!backendStatus.connected && !backendStatus.checking && (
          <div className="mt-4 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            <p className="font-medium">⚠️ Backend Connection Failed</p>
            <p className="text-sm mt-1">
              {backendStatus.error || "Cannot connect to backend at http://localhost:8081"}
            </p>
          </div>
        )}
      </div>

      {/* Save message removed - no longer needed since data is saved automatically */}

      {/* Seller Profile Section */}
      <div className="bg-white rounded-lg shadow mb-6 relative">
        <div className="p-4 border-b border-gray-200 bg-gradient-to-r from-blue-50 to-purple-50">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-xl font-semibold text-gray-800">Your Company Profile</h2>
              <p className="text-sm text-gray-600 mt-1">Tell us about your company to improve matching accuracy</p>
            </div>
            <button
              onClick={fillExampleData}
              className="text-sm bg-white hover:bg-gray-50 text-gray-700 px-4 py-2 rounded-lg border border-gray-300 transition shadow-sm"
            >
              🧪 Fill Navoy Example (Hunter Testing)
            </button>
          </div>
        </div>

        {/* Company Basics */}
        <div className="border-b border-gray-200">
          <SectionHeader
            icon={Building2}
            title="Company Basics"
            expanded={expandedSellerSections.basics}
            onToggle={() => toggleSellerSection('basics')}
          />
          {expandedSellerSections.basics && (
            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Company Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="companyName"
                  value={sellerProfile.companyName}
                  onChange={handleSellerChange}
                  placeholder="e.g., Navoy"
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    errors.companyName ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {errors.companyName && <p className="text-red-500 text-xs mt-1">{errors.companyName}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Industry <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="industry"
                  value={sellerProfile.industry}
                  onChange={handleSellerChange}
                  placeholder="e.g., Travel Tech, Fintech, AI SaaS"
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    errors.industry ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {errors.industry && <p className="text-red-500 text-xs mt-1">{errors.industry}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Company Size (Employees) <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  name="companySize"
                  value={sellerProfile.companySize}
                  onChange={handleSellerChange}
                  placeholder="e.g., 200"
                  min="1"
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    errors.companySize ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {errors.companySize && <p className="text-red-500 text-xs mt-1">{errors.companySize}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Founded Year
                </label>
                <input
                  type="number"
                  name="foundedYear"
                  value={sellerProfile.foundedYear}
                  onChange={handleSellerChange}
                  placeholder="e.g., 2018"
                  min="1900"
                  max={new Date().getFullYear()}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Headquarters Region
                </label>
                <input
                  type="text"
                  name="headquartersRegion"
                  value={sellerProfile.headquartersRegion}
                  onChange={handleSellerChange}
                  placeholder="e.g., US, Europe, MENA"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          )}
        </div>

        {/* Product/Offering */}
        <div className="border-b border-gray-200">
          <SectionHeader
            icon={Target}
            title="Product / Offering"
            expanded={expandedSellerSections.product}
            onToggle={() => toggleSellerSection('product')}
          />
          {expandedSellerSections.product && (
            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Value Proposition Keywords <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="valuePropositionKeywords"
                  value={sellerProfile.valuePropositionKeywords}
                  onChange={handleSellerChange}
                  placeholder="e.g., AI automation, personalization, SaaS (comma-separated)"
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    errors.valuePropositionKeywords ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {errors.valuePropositionKeywords && <p className="text-red-500 text-xs mt-1">{errors.valuePropositionKeywords}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Target Customer Segment
                </label>
                <select
                  name="targetCustomerSegment"
                  value={sellerProfile.targetCustomerSegment}
                  onChange={handleSellerChange}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select...</option>
                  <option value="Enterprises">Enterprises</option>
                  <option value="SMBs">SMBs</option>
                  <option value="B2B startups">B2B startups</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Price Tier
                </label>
                <select
                  name="priceTier"
                  value={sellerProfile.priceTier}
                  onChange={handleSellerChange}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select...</option>
                  <option value="Premium">Premium</option>
                  <option value="Mid-market">Mid-market</option>
                  <option value="Freemium">Freemium</option>
                </select>
              </div>
            </div>
          )}
        </div>

        {/* Technology Stack */}
        <div className="border-b border-gray-200">
          <SectionHeader
            icon={Code}
            title="Technology Stack"
            expanded={expandedSellerSections.tech}
            onToggle={() => toggleSellerSection('tech')}
          />
          {expandedSellerSections.tech && (
            <div className="p-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Tech Stack
                </label>
                <input
                  type="text"
                  name="techStack"
                  value={sellerProfile.techStack}
                  onChange={handleSellerChange}
                  placeholder="e.g., React, Node.js, PostgreSQL, AWS (comma-separated)"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          )}
        </div>

        {/* Go-to-Market */}
        <div>
          <SectionHeader
            icon={TrendingUp}
            title="Go-to-Market"
            expanded={expandedSellerSections.gtm}
            onToggle={() => toggleSellerSection('gtm')}
          />
          {expandedSellerSections.gtm && (
            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Sales Model
                </label>
                <select
                  name="salesModel"
                  value={sellerProfile.salesModel}
                  onChange={handleSellerChange}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select...</option>
                  <option value="Inbound">Inbound</option>
                  <option value="Outbound">Outbound</option>
                  <option value="Product-Led Growth (PLG)">Product-Led Growth (PLG)</option>
                  <option value="Channel / Partner">Channel / Partner</option>
                  <option value="Enterprise Sales">Enterprise Sales</option>
                  <option value="Inside Sales">Inside Sales</option>
                  <option value="Self-Serve">Self-Serve</option>
                </select>
                {sellerProfile.salesModel && (
                  <p className="text-xs text-gray-600 mt-1 italic">
                    {sellerProfile.salesModel === "Inbound" && "Customers come to you (through marketing, SEO, ads)"}
                    {sellerProfile.salesModel === "Outbound" && "You reach out (cold emails, calls, LinkedIn)"}
                    {sellerProfile.salesModel === "Product-Led Growth (PLG)" && "Product sells itself via free trials or freemium"}
                    {sellerProfile.salesModel === "Channel / Partner" && "You sell through resellers or distributors"}
                    {sellerProfile.salesModel === "Enterprise Sales" && "Long sales cycles, high-value contracts"}
                    {sellerProfile.salesModel === "Inside Sales" && "Sales done remotely by SDRs/BDRs"}
                    {sellerProfile.salesModel === "Self-Serve" && "Fully automated, users buy directly online"}
                  </p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Target Regions <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="targetRegions"
                  value={sellerProfile.targetRegions}
                  onChange={handleSellerChange}
                  placeholder="e.g., US, Canada, Europe (comma-separated)"
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    errors.targetRegions ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {errors.targetRegions && <p className="text-red-500 text-xs mt-1">{errors.targetRegions}</p>}
              </div>
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Company Description <span className="text-red-500">*</span>
                </label>
                <textarea
                  name="description"
                  value={sellerProfile.description}
                  onChange={handleSellerChange}
                  placeholder="Describe what your company does, your products/services, and your target market..."
                  rows="3"
                  required
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    errors.description ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {errors.description && <p className="text-red-500 text-xs mt-1">{errors.description}</p>}
              </div>
            </div>
          )}
        </div>
        
        {/* Save button removed - data is saved automatically when generating leads */}
      </div>

      {/* Prospect Criteria Section */}
      <div className="bg-white rounded-lg shadow mb-6 relative">
        <div className="p-4 border-b border-gray-200 bg-gradient-to-r from-green-50 to-blue-50">
          <div>
            <h2 className="text-xl font-semibold text-gray-800">Prospect Search Criteria</h2>
            <p className="text-sm text-gray-600 mt-1">Define precise criteria to find your ideal prospects</p>
          </div>
        </div>

        {/* General */}
        <div className="border-b border-gray-200">
          <SectionHeader
            icon={Building2}
            title="General"
            expanded={expandedCriteriaSections.general}
            onToggle={() => toggleCriteriaSection('general')}
          />
          {expandedCriteriaSections.general && (
            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                  Company Name
                  <div className="group relative">
                    <Info size={14} className="text-gray-400 hover:text-blue-600 cursor-help" />
                    <div className="absolute left-1/2 transform -translate-x-1/2 top-6 w-64 p-2 bg-gray-900 text-white text-xs rounded shadow-lg opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                      Use this field to find a specific company or person within a company
                    </div>
                  </div>
                </label>
                <input
                  type="text"
                  name="companyName"
                  value={prospectCriteria.companyName}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., Expedia"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-1">
                  Domain
                  <div className="group relative">
                    <Info size={14} className="text-gray-400 hover:text-blue-600 cursor-help" />
                    <div className="absolute left-1/2 transform -translate-x-1/2 top-6 w-64 p-2 bg-gray-900 text-white text-xs rounded shadow-lg opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                      Use this field to find a specific company or person by their website domain
                    </div>
                  </div>
                </label>
                <input
                  type="text"
                  name="domain"
                  value={prospectCriteria.domain}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., expedia.com"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Industry <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="industry"
                  value={prospectCriteria.industry}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., Travel tech, Hospitality"
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    criteriaErrors.industry ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {criteriaErrors.industry && <p className="text-red-500 text-xs mt-1">{criteriaErrors.industry}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Min Company Size <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  name="minSize"
                  value={prospectCriteria.minSize}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., 10"
                  min="1"
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    criteriaErrors.minSize ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {criteriaErrors.minSize && <p className="text-red-500 text-xs mt-1">{criteriaErrors.minSize}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Max Company Size <span className="text-red-500">*</span>
                </label>
                <input
                  type="number"
                  name="maxSize"
                  value={prospectCriteria.maxSize}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., 500"
                  min="1"
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    criteriaErrors.maxSize ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {criteriaErrors.maxSize && <p className="text-red-500 text-xs mt-1">{criteriaErrors.maxSize}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Min Founded Year
                </label>
                <input
                  type="number"
                  name="minFoundedYear"
                  value={prospectCriteria.minFoundedYear}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., 2015"
                  min="1900"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Max Founded Year
                </label>
                <input
                  type="number"
                  name="maxFoundedYear"
                  value={prospectCriteria.maxFoundedYear}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., 2024"
                  min="1900"
                  max={new Date().getFullYear()}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          )}
        </div>

        {/* Financials */}
        <div className="border-b border-gray-200">
          <SectionHeader
            icon={DollarSign}
            title="Financials"
            expanded={expandedCriteriaSections.financials}
            onToggle={() => toggleCriteriaSection('financials')}
          />
          {expandedCriteriaSections.financials && (
            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Funding Stage
                </label>
                <select
                  name="fundingStage"
                  value={prospectCriteria.fundingStage}
                  onChange={handleCriteriaChange}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select...</option>
                  <option value="Seed">Seed</option>
                  <option value="Series A">Series A</option>
                  <option value="Series B">Series B</option>
                  <option value="Series C">Series C</option>
                  <option value="IPO">IPO</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Annual Revenue Range
                </label>
                <input
                  type="text"
                  name="annualRevenueRange"
                  value={prospectCriteria.annualRevenueRange}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., $1M–$10M"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Growth Rate
                </label>
                <input
                  type="text"
                  name="growthRate"
                  value={prospectCriteria.growthRate}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., >10% YoY"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
          )}
        </div>

        {/* Technology */}
        <div className="border-b border-gray-200">
          <SectionHeader
            icon={Code}
            title="Technology"
            expanded={expandedCriteriaSections.technology}
            onToggle={() => toggleCriteriaSection('technology')}
          />
          {expandedCriteriaSections.technology && (
            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Tech Used (Must-have)
                </label>
                <input
                  type="text"
                  name="techUsed"
                  value={prospectCriteria.techUsed}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., React, AWS, Stripe (comma-separated)"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <p className="text-xs text-gray-500 mt-1">Only companies using ALL of these will be shown</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Tech Category
                </label>
                <select
                  name="techCategory"
                  value={prospectCriteria.techCategory}
                  onChange={handleCriteriaChange}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select...</option>
                  <option value="Cloud Infrastructure">Cloud Infrastructure</option>
                  <option value="CRM">CRM</option>
                  <option value="Payment Gateway">Payment Gateway</option>
                  <option value="Database">Database</option>
                  <option value="Frontend Framework">Frontend Framework</option>
                </select>
              </div>
            </div>
          )}
        </div>

        {/* Decision Makers */}
        <div className="border-b border-gray-200">
          <SectionHeader
            icon={Users}
            title="Decision Makers"
            expanded={expandedCriteriaSections.decisionMakers}
            onToggle={() => toggleCriteriaSection('decisionMakers')}
          />
          {expandedCriteriaSections.decisionMakers && (
            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Target Roles
                </label>
                <input
                  type="text"
                  name="targetRoles"
                  value={prospectCriteria.targetRoles}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., CTO, VP Engineering, Head of AI (comma-separated)"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Seniority Level
                </label>
                <select
                  name="seniorityLevel"
                  value={prospectCriteria.seniorityLevel}
                  onChange={handleCriteriaChange}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select...</option>
                  <option value="C-level">C-level</option>
                  <option value="VP">VP</option>
                  <option value="Director">Director</option>
                  <option value="Manager">Manager</option>
                </select>
              </div>
            </div>
          )}
        </div>

        {/* Geography */}
        <div className="border-b border-gray-200">
          <SectionHeader
            icon={MapPin}
            title="Geography"
            expanded={expandedCriteriaSections.geography}
            onToggle={() => toggleCriteriaSection('geography')}
          />
          {expandedCriteriaSections.geography && (
            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Headquarters Region
                </label>
                <input
                  type="text"
                  name="headquartersRegion"
                  value={prospectCriteria.headquartersRegion}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., North America, Europe, MENA"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  HQ Country
                </label>
                <input
                  type="text"
                  name="hqCountry"
                  value={prospectCriteria.hqCountry}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., US, UK, France"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Remote Friendly
                </label>
                <select
                  name="remoteFriendly"
                  value={prospectCriteria.remoteFriendly}
                  onChange={handleCriteriaChange}
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Any</option>
                  <option value="true">Yes</option>
                  <option value="false">No</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Target Regions <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  name="regions"
                  value={prospectCriteria.regions}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., US, EU, APAC (comma-separated)"
                  className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
                    criteriaErrors.regions ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
                  }`}
                />
                {criteriaErrors.regions && <p className="text-red-500 text-xs mt-1">{criteriaErrors.regions}</p>}
              </div>
            </div>
          )}
        </div>

        {/* Behavioral/Intent */}
        <div>
          <SectionHeader
            icon={TrendingUp}
            title="Behavioral / Intent Data"
            expanded={expandedCriteriaSections.behavioral}
            onToggle={() => toggleCriteriaSection('behavioral')}
          />
          {expandedCriteriaSections.behavioral && (
            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Hiring Trends
                </label>
                <input
                  type="text"
                  name="hiringTrends"
                  value={prospectCriteria.hiringTrends}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., Hiring in engineering"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Recent Tech Adoption
                </label>
                <input
                  type="text"
                  name="recentTechAdoption"
                  value={prospectCriteria.recentTechAdoption}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., Recently adopted Kubernetes"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Keyword Mentions
                </label>
                <input
                  type="text"
                  name="keywordMentions"
                  value={prospectCriteria.keywordMentions}
                  onChange={handleCriteriaChange}
                  placeholder="e.g., AI, automation (from news or job postings, comma-separated)"
                  className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <p className="text-xs text-gray-500 mt-1">Companies mentioning these keywords will score higher</p>
              </div>
            </div>
          )}
        </div>
        
        {/* Save button removed - data is saved automatically when generating leads */}
      </div>

      {/* Generate Leads Button */}
      <div className="mb-6" data-tour="generate-leads-button">
        <button
          onClick={handleGenerateLeads}
          disabled={loading}
          className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 disabled:from-gray-400 disabled:to-gray-400 disabled:cursor-not-allowed text-white px-8 py-3 rounded-lg flex items-center gap-2 transition text-lg font-semibold shadow-lg hover:shadow-xl relative overflow-hidden"
        >
          {loading && (
            <span className="absolute inset-0 bg-gradient-to-r from-blue-700 to-purple-700 animate-pulse"></span>
          )}
          <span className="relative z-10 flex items-center gap-2">
            <Sparkles size={24} className={loading ? "animate-spin" : ""} />
            {loading ? "Generating..." : "Generate Leads"}
          </span>
        </button>
      </div>


      {matchedProspects.length > 0 && (
      <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="p-4 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
            <div>
              <h3 className="text-lg font-semibold text-gray-800">
                Matched Prospects ({matchedProspects.length})
              </h3>
              <p className="text-sm text-gray-600">Sorted by match score (highest first)</p>
          </div>
            <button
              onClick={downloadCSV}
              className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg transition shadow-sm hover:shadow-md"
            >
              <Download size={18} />
              <span>Download CSV</span>
            </button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Match Score</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Company</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">First Name</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Last Name</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Position</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Domain</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Industry</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Size</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Regions</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stack</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {matchedProspects.map((item) => {
                  const prospect = item.prospect || item;
                  const score = item.score || 0;
                  return (
                    <tr key={prospect.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4">
                        <span className={`px-2 py-1 rounded text-xs font-semibold ${
                          score >= 70 ? 'bg-green-100 text-green-800' :
                          score >= 40 ? 'bg-yellow-100 text-yellow-800' :
                          'bg-gray-100 text-gray-800'
                        }`}>
                          {score.toFixed(1)}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm font-medium text-gray-900">{prospect.company}</td>
                      <td className="px-6 py-4 text-sm text-gray-500">{prospect.firstName || "-"}</td>
                      <td className="px-6 py-4 text-sm text-gray-500">{prospect.lastName || "-"}</td>
                      <td className="px-6 py-4 text-sm text-gray-500">{prospect.position || "-"}</td>
                      <td className="px-6 py-4 text-sm text-blue-600">{prospect.email || "-"}</td>
                      <td className="px-6 py-4 text-sm text-gray-500">{prospect.domain || "-"}</td>
                      <td className="px-6 py-4 text-sm text-gray-500">{prospect.industry || "-"}</td>
                      <td className="px-6 py-4 text-sm text-gray-500">{prospect.size || "-"}</td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {prospect.regions && prospect.regions.length > 0 ? prospect.regions.join(", ") : "-"}
                      </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                        {prospect.stack && prospect.stack.length > 0 ? prospect.stack.join(", ") : "-"}
                    </td>
                  </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!loading && hasSearched && matchedProspects.length === 0 && (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <div className="max-w-md mx-auto">
            <div className="mb-4">
              <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No Matches Found</h3>
            <p className="text-gray-600 mb-4">
              No prospects were found matching your search criteria. Try broadening your filters.
            </p>
          </div>
          </div>
        )}

      {!loading && !hasSearched && (
        <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
          <p>Fill in your company information and prospect criteria, then click "Generate Leads" to find matching companies.</p>
      </div>
      )}
    </div>
    </>
  );
}
