import { 
  Bold, Italic, Underline, Link, List, ListOrdered, 
  Eye, Undo2, Redo2, Type, Palette, Highlighter,
  User, Building2, Briefcase, Mail, Hash, FileText,
  CheckCircle, AlertCircle, Info, Sparkles, Loader, X,
  Smartphone, Monitor, RefreshCw
} from "lucide-react";
import { useEffect, useRef, useState, useCallback } from "react";
import { SketchPicker } from 'react-color';
import { campaignApi } from "../../../services/campaignApi";

const AVAILABLE_VARIABLES = [
  { key: 'firstName', label: 'First Name', icon: User },
  { key: 'lastName', label: 'Last Name', icon: User },
  { key: 'company', label: 'Company', icon: Building2 },
  { key: 'position', label: 'Position', icon: Briefcase },
  { key: 'email', label: 'Email', icon: Mail },
  { key: 'domain', label: 'Domain', icon: Hash },
  { key: 'industry', label: 'Industry', icon: Building2 },
  { key: 'personalizationHook', label: 'Personalization Hook', icon: FileText },
];

export default function EmailComposeArea({
  emailSubject,
  setEmailSubject,
  emailContent,
  setEmailContent,
  campaignName,
  campaignContext,
  campaignId,
  contactId,
  csvColumns = []
}) {
  const editorRef = useRef(null);
  const [isEditorFocused, setIsEditorFocused] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [showVariables, setShowVariables] = useState(false);
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [showHighlightPicker, setShowHighlightPicker] = useState(false);
  const [history, setHistory] = useState([]);
  const [historyIndex, setHistoryIndex] = useState(-1);
  const [showAIFeedback, setShowAIFeedback] = useState(true);
  const [aiFeedback, setAiFeedback] = useState({ subject: [], content: [] });
  const [isAIGenerating, setIsAIGenerating] = useState(false);
  const feedbackTimeoutRef = useRef(null);
  const [previewMode, setPreviewMode] = useState('desktop'); // 'desktop', 'phone'
  const [phoneNotificationType, setPhoneNotificationType] = useState('lockscreen'); // 'lockscreen', 'popup'
  const [emailClient, setEmailClient] = useState('gmail'); // 'gmail', 'outlook', 'apple-mail'
  const [colorPickerColor, setColorPickerColor] = useState({ hex: '#000000' });
  const [highlightPickerColor, setHighlightPickerColor] = useState({ hex: '#FEF08A' });

  // Backend preview state (spintax + {{variable}} rotation)
  const [backendPreviewSubject, setBackendPreviewSubject] = useState(null);
  const [backendPreviewBody, setBackendPreviewBody] = useState(null);
  const [backendPreviewLoading, setBackendPreviewLoading] = useState(false);
  const [backendPreviewError, setBackendPreviewError] = useState(null);

  // Close color pickers when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (showColorPicker && !event.target.closest('.color-picker-container')) {
        setShowColorPicker(false);
      }
      if (showHighlightPicker && !event.target.closest('.highlight-picker-container')) {
        setShowHighlightPicker(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showColorPicker, showHighlightPicker]);

  // Load backend preview (spintax + {{variable}} rotation)
  const loadBackendPreview = useCallback(async () => {
    if (!campaignId || !emailSubject || !emailContent) {
      setBackendPreviewSubject(null);
      setBackendPreviewBody(null);
      setBackendPreviewError(null);
      return;
    }

    try {
      setBackendPreviewLoading(true);
      setBackendPreviewError(null);

      const seed = Date.now();

      const preview = await campaignApi.previewEmail(
        campaignId,
        emailSubject,
        emailContent,
        contactId || null,
        seed
      );

      setBackendPreviewSubject(preview.subject || null);
      setBackendPreviewBody(preview.bodyPreview || preview.body || null);
    } catch (error) {
      console.error("Step2 backend preview failed:", error);
      setBackendPreviewSubject(null);
      setBackendPreviewBody(null);
      setBackendPreviewError("Backend preview unavailable. Showing template only.");
    } finally {
      setBackendPreviewLoading(false);
    }
  }, [campaignId, emailSubject, emailContent, contactId]);

  // Auto-refresh backend preview when preview is open and content changes
  useEffect(() => {
    if (showPreview) {
      loadBackendPreview();
    }
  }, [showPreview, loadBackendPreview, emailSubject, emailContent]);

  // Calculate stats
  const getTextStats = (html) => {
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = html;
    const text = tempDiv.textContent || tempDiv.innerText || '';
    const words = text.trim().split(/\s+/).filter(word => word.length > 0);
    const characters = text.length;
    const charactersNoSpaces = text.replace(/\s/g, '').length;
    return { words: words.length, characters, charactersNoSpaces };
  };

  const contentStats = getTextStats(emailContent);
  const subjectStats = emailSubject.length;

  // Email length recommendations
  const getEmailRecommendations = () => {
    const recommendations = [];
    if (contentStats.words > 200) {
      recommendations.push({ type: 'warning', text: 'Email is too long (>200 words). Keep it under 100 words for better response rates.' });
    } else if (contentStats.words > 100) {
      recommendations.push({ type: 'info', text: 'Email is getting long. Consider shortening to under 100 words.' });
    } else if (contentStats.words < 50 && contentStats.words > 0) {
      recommendations.push({ type: 'success', text: 'Great length! Short emails (50-100 words) get better response rates.' });
    }
    
    if (subjectStats > 60) {
      recommendations.push({ type: 'warning', text: 'Subject line is too long (>60 chars). Keep it under 50 characters.' });
    } else if (subjectStats > 50) {
      recommendations.push({ type: 'info', text: 'Subject line is getting long. Consider shortening to under 50 characters.' });
    } else if (subjectStats > 0 && subjectStats <= 50) {
      recommendations.push({ type: 'success', text: 'Perfect subject length! (Under 50 characters)' });
    }

    return recommendations;
  };

  const recommendations = getEmailRecommendations();

  // Format text functions
  const formatText = (command, value = null) => {
    // Save to history before formatting
    saveToHistory();
    document.execCommand(command, false, value);
    if (editorRef.current) {
      editorRef.current.focus();
      handleEditorChange();
    }
  };

  const insertVariable = (variable) => {
    saveToHistory();
    const variableText = `{{${variable}}}`;
    const selection = window.getSelection();
    if (selection.rangeCount > 0) {
      const range = selection.getRangeAt(0);
      range.deleteContents();
      const textNode = document.createTextNode(variableText);
      range.insertNode(textNode);
      range.setStartAfter(textNode);
      range.collapse(true);
      selection.removeAllRanges();
      selection.addRange(range);
    } else if (editorRef.current) {
      editorRef.current.focus();
      document.execCommand('insertText', false, variableText);
    }
    handleEditorChange();
    setShowVariables(false);
  };

  const insertLink = () => {
    const url = prompt('Enter the URL:');
    if (url) {
      saveToHistory();
      formatText('createLink', url);
    }
  };

  const insertSignature = () => {
    saveToHistory();
    const signature = `
<br><br>
Best regards,<br>
[Your Name]<br>
[Your Title]<br>
[Your Company]`;
    if (editorRef.current) {
      editorRef.current.focus();
      document.execCommand('insertHTML', false, signature);
      handleEditorChange();
    }
  };

  // History management for undo/redo
  const saveToHistory = () => {
    if (editorRef.current) {
      const currentContent = editorRef.current.innerHTML;
      const newHistory = history.slice(0, historyIndex + 1);
      newHistory.push(currentContent);
      if (newHistory.length > 50) {
        newHistory.shift();
      } else {
        setHistoryIndex(newHistory.length - 1);
      }
      setHistory(newHistory);
    }
  };

  const undo = () => {
    if (historyIndex > 0 && editorRef.current) {
      const newIndex = historyIndex - 1;
      editorRef.current.innerHTML = history[newIndex];
      setHistoryIndex(newIndex);
      handleEditorChange();
    }
  };

  const redo = () => {
    if (historyIndex < history.length - 1 && editorRef.current) {
      const newIndex = historyIndex + 1;
      editorRef.current.innerHTML = history[newIndex];
      setHistoryIndex(newIndex);
      handleEditorChange();
    }
  };

  // Sync emailContent with editor content
  useEffect(() => {
    if (editorRef.current && !showPreview) {
      // Only sync if editor is visible and content differs
      if (editorRef.current.innerHTML !== emailContent) {
        if (!isEditorFocused) {
          editorRef.current.innerHTML = emailContent || '';
          // Reset history when content is set externally
          setHistory([emailContent || '']);
          setHistoryIndex(0);
        }
      }
    }
  }, [emailContent, isEditorFocused, showPreview]);

  // AI Feedback Generation (Mock - will be replaced with actual AI service)
  const generateAIFeedback = useCallback(async (subject, content, campaignName, campaignContext) => {
    setIsAIGenerating(true);
    
    // TODO: Replace with actual AI service call
    // const response = await aiService.generateFeedback({
    //   subject,
    //   content,
    //   campaignName,
    //   campaignContext
    // });
    
    // Mock feedback for now
    await new Promise(resolve => setTimeout(resolve, 800));
    
    const subjectFeedback = [];
    const contentFeedback = [];
    
    // Subject line feedback
    if (subject.length > 0) {
      if (subject.length > 50) {
        subjectFeedback.push({
          type: 'warning',
          text: 'Subject line is too long. Keep it under 50 characters for better open rates.',
          suggestion: subject.substring(0, 47) + '...'
        });
      }
      if (!subject.match(/{{firstName|{{company|{{position/)) {
        subjectFeedback.push({
          type: 'info',
          text: 'Consider adding personalization variables like {{firstName}} or {{company}} to increase open rates.',
        });
      }
      if (subject.toLowerCase().includes('free') || subject.toLowerCase().includes('click here')) {
        subjectFeedback.push({
          type: 'warning',
          text: 'Avoid spam trigger words like "free" or "click here" in subject lines.',
        });
      }
    }
    
    // Content feedback
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = content;
    const textContent = tempDiv.textContent || tempDiv.innerText || '';
    const wordCount = textContent.trim().split(/\s+/).filter(word => word.length > 0).length;
    
    if (wordCount > 0) {
      if (wordCount > 100) {
        contentFeedback.push({
          type: 'warning',
          text: `Email is ${wordCount} words. Cold emails perform best at 50-100 words. Consider shortening.`,
        });
      }
      
      if (!textContent.match(/{{firstName|{{company|{{position/)) {
        contentFeedback.push({
          type: 'info',
          text: 'Add personalization variables ({{firstName}}, {{company}}) to make your email more relevant.',
        });
      }
      
      if (!textContent.match(/\?/)) {
        contentFeedback.push({
          type: 'info',
          text: 'Consider adding a question to engage the prospect and encourage a response.',
        });
      }
      
      if (textContent.match(/I|we|our|my/gi) && (textContent.match(/I|we|our|my/gi) || []).length > 5) {
        contentFeedback.push({
          type: 'warning',
          text: 'Too many "I/we/our" statements. Focus on the prospect\'s pain points instead.',
        });
      }
      
      if (campaignName && campaignContext) {
        contentFeedback.push({
          type: 'info',
          text: `Based on campaign "${campaignName}", ensure your message aligns with the target audience's needs.`,
        });
      }
    }
    
    setAiFeedback({ subject: subjectFeedback, content: contentFeedback });
    setIsAIGenerating(false);
  }, []);

  // Debounced AI feedback generation
  useEffect(() => {
    if (feedbackTimeoutRef.current) {
      clearTimeout(feedbackTimeoutRef.current);
    }
    
    feedbackTimeoutRef.current = setTimeout(() => {
      if (emailSubject || emailContent) {
        generateAIFeedback(emailSubject, emailContent, campaignName, campaignContext);
      }
    }, 1500); // Wait 1.5 seconds after user stops typing
    
    return () => {
      if (feedbackTimeoutRef.current) {
        clearTimeout(feedbackTimeoutRef.current);
      }
    };
  }, [emailSubject, emailContent, campaignName, campaignContext, generateAIFeedback]);

  // Handle editor content changes
  const handleEditorChange = () => {
    if (editorRef.current) {
      const content = editorRef.current.innerHTML;
      setEmailContent(content);
      // Auto-save to history on typing (debounced)
      if (isEditorFocused) {
        const timeoutId = setTimeout(() => {
          saveToHistory();
        }, 500);
        return () => clearTimeout(timeoutId);
      }
    }
  };

  // AI Writing Assistant - Generate email content
  const handleAIWriting = async () => {
    setIsAIGenerating(true);
    // TODO: Replace with actual AI service call
    // const response = await aiService.generateEmail({
    //   campaignName,
    //   campaignContext,
    //   emailType: 'initial' // or 'follow-up'
    // });
    
    // Mock AI generation
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    const mockSubject = campaignName 
      ? `Quick question about ${campaignName}`
      : 'Quick question about your business';
    
    const mockContent = `Hi {{firstName}},

I noticed {{company}} is in the {{industry}} space. I'd love to share how we've helped similar companies [achieve specific result].

Would you be open to a quick 15-minute conversation this week?

Best regards,
[Your Name]`;
    
    if (editorRef.current) {
      editorRef.current.innerHTML = mockContent;
      setEmailContent(mockContent);
      setEmailSubject(mockSubject);
      setIsAIGenerating(false);
    }
  };

  // Backend + local preview helpers
  const getPreviewSubject = () => {
    if (backendPreviewSubject) return backendPreviewSubject;
    // Fallback: show raw subject with placeholders
    return emailSubject || "(No subject yet)";
  };

  const getPreviewContent = () => {
    // Prefer backend-rendered HTML when available
    if (backendPreviewBody) {
      return backendPreviewBody;
    }
    // Fallback: simple local replacement to at least show where variables go
    let preview = emailContent || "";
    AVAILABLE_VARIABLES.forEach(variable => {
      const regex = new RegExp(`\\{\\{${variable.key}\\}\\}`, 'g');
      preview = preview.replace(regex, `[${variable.label}]`);
    });
    return preview;
  };

  const colors = [
    { name: 'Black', value: '#000000' },
    { name: 'Gray', value: '#6B7280' },
    { name: 'Blue', value: '#2563EB' },
    { name: 'Green', value: '#10B981' },
    { name: 'Red', value: '#EF4444' },
    { name: 'Purple', value: '#8B5CF6' },
  ];

  const highlights = [
    { name: 'Yellow', value: '#FEF08A' },
    { name: 'Green', value: '#D1FAE5' },
    { name: 'Blue', value: '#DBEAFE' },
    { name: 'Pink', value: '#FCE7F3' },
  ];

  return (
    <div className="flex-1 min-w-0 border border-gray-300 rounded-lg shadow-sm bg-white">
      {/* Email Header */}
      <div className="border-b border-gray-200 p-4 bg-gray-50">
        <div className="flex items-center gap-2 mb-3">
          <span className="text-sm text-gray-600">From:</span>
          <span className="text-sm font-medium text-gray-900">
            Your Email
          </span>
        </div>
        <div className="flex items-center gap-2 mb-3">
          <span className="text-sm text-gray-600">To:</span>
          <span className="text-sm text-gray-500">
            example@example.com
          </span>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-600">
            Subject:
          </span>
          <div className="flex-1 relative">
            <input
              type="text"
              value={emailSubject}
              onChange={(e) =>
                setEmailSubject(e.target.value)
              }
              placeholder="Enter email subject..."
              className="w-full bg-transparent border border-gray-300/50 backdrop-blur-sm bg-white/30 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-400/50 px-3 py-2 rounded-lg shadow-sm transition-all"
            />
            {!emailSubject && (
              <span className="absolute right-3 top-1/2 transform -translate-y-1/2 text-red-500 text-xs">
                *
              </span>
            )}
            <div className="absolute right-3 top-1/2 transform -translate-y-1/2 text-xs text-gray-400">
              {subjectStats}/50
            </div>
          </div>
        </div>
      </div>

      {/* Email Body */}
      <div className="p-4">
        {/* Stats and Actions Bar */}
        <div className="flex items-center justify-between mb-3 pb-2 border-b border-gray-200">
          <div className="flex items-center gap-4 text-xs text-gray-600">
            <span>{contentStats.words} words</span>
            <span>{contentStats.characters} chars</span>
            {recommendations.length > 0 && (
              <div className="flex items-center gap-1">
                {recommendations.some(r => r.type === 'warning') && (
                  <AlertCircle size={14} className="text-yellow-500" />
                )}
                {recommendations.every(r => r.type === 'success') && (
                  <CheckCircle size={14} className="text-green-500" />
                )}
              </div>
            )}
          </div>
          <div className="flex items-center gap-2">
            {showPreview && (
              <>
                <div className="flex items-center gap-1 border border-gray-300 rounded-lg p-1">
                  <button
                    type="button"
                    onClick={() => setPreviewMode('desktop')}
                    className={`px-2 py-1 text-xs rounded transition-colors flex items-center gap-1 ${
                      previewMode === 'desktop'
                        ? 'bg-blue-600 text-white'
                        : 'bg-transparent text-gray-600 hover:bg-gray-100'
                    }`}
                    title="Desktop Preview"
                  >
                    <Monitor size={14} />
                  </button>
                  <button
                    type="button"
                    onClick={() => setPreviewMode('phone')}
                    className={`px-2 py-1 text-xs rounded transition-colors flex items-center gap-1 ${
                      previewMode === 'phone'
                        ? 'bg-blue-600 text-white'
                        : 'bg-transparent text-gray-600 hover:bg-gray-100'
                    }`}
                    title="Phone Notification"
                  >
                    <Smartphone size={14} />
                  </button>
                </div>

                {/* Refresh Sample (backend spintax + variable rotation) */}
                {campaignId && (
                  <button
                    type="button"
                    onClick={loadBackendPreview}
                    disabled={backendPreviewLoading}
                    className="px-2 py-1 text-xs rounded border border-gray-300 text-gray-700 hover:bg-gray-100 disabled:opacity-50 flex items-center gap-1"
                    title="Refresh sample with new contact / spintax"
                  >
                    <RefreshCw size={14} className={backendPreviewLoading ? "animate-spin" : ""} />
                    <span>Refresh sample</span>
                  </button>
                )}
              </>
            )}
            {campaignId && (
              <button
                type="button"
                onClick={async () => {
                  try {
                    if (editorRef.current) {
                      const currentContent = editorRef.current.innerHTML;
                      setEmailContent(currentContent);
                      await campaignApi.saveCampaignEmail(
                        campaignId,
                        emailSubject,
                        currentContent
                      );
                    }
                  } catch (error) {
                    console.error("Failed to save campaign email from Step2:", error);
                  }
                }}
                className="px-3 py-1.5 text-xs rounded border border-gray-300 text-gray-700 hover:bg-gray-100 flex items-center gap-1"
              >
                <CheckCircle size={14} className="text-green-500" />
                <span>Save Email</span>
              </button>
            )}
            <button
              type="button"
              onClick={() => {
                // Save editor content before switching to preview
                if (editorRef.current && !showPreview) {
                  const currentContent = editorRef.current.innerHTML;
                  setEmailContent(currentContent);
                }
                setShowPreview(!showPreview);
              }}
              className={`px-3 py-1.5 text-xs rounded transition-colors flex items-center gap-1 ${
                showPreview 
                  ? 'bg-blue-600 text-white' 
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              <Eye size={14} />
              {showPreview ? 'Hide Preview' : 'Preview'}
            </button>
          </div>
        </div>

        {!showPreview ? (
          <>
            <div className="border border-gray-200 rounded min-h-[400px] overflow-hidden">
              {/* Enhanced Toolbar */}
              <div className="border-b border-gray-200 bg-gray-50 p-2 flex items-center gap-1 flex-wrap">
                {/* Undo/Redo */}
                <button
                  type="button"
                  onClick={undo}
                  disabled={historyIndex <= 0}
                  className="p-2 hover:bg-gray-200 rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  title="Undo"
                >
                  <Undo2 size={16} />
                </button>
                <button
                  type="button"
                  onClick={redo}
                  disabled={historyIndex >= history.length - 1}
                  className="p-2 hover:bg-gray-200 rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  title="Redo"
                >
                  <Redo2 size={16} />
                </button>
                <div className="w-px h-6 bg-gray-300 mx-1"></div>

                {/* Text Formatting */}
                <button
                  type="button"
                  onClick={() => formatText('bold')}
                  className="p-2 hover:bg-gray-200 rounded transition-colors"
                  title="Bold"
                >
                  <Bold size={16} />
                </button>
                <button
                  type="button"
                  onClick={() => formatText('italic')}
                  className="p-2 hover:bg-gray-200 rounded transition-colors"
                  title="Italic"
                >
                  <Italic size={16} />
                </button>
                <button
                  type="button"
                  onClick={() => formatText('underline')}
                  className="p-2 hover:bg-gray-200 rounded transition-colors"
                  title="Underline"
                >
                  <Underline size={16} />
                </button>
                <div className="w-px h-6 bg-gray-300 mx-1"></div>

                {/* Lists */}
                <button
                  type="button"
                  onClick={() => formatText('insertUnorderedList')}
                  className="p-2 hover:bg-gray-200 rounded transition-colors"
                  title="Bullet List"
                >
                  <List size={16} />
                </button>
                <button
                  type="button"
                  onClick={() => formatText('insertOrderedList')}
                  className="p-2 hover:bg-gray-200 rounded transition-colors"
                  title="Numbered List"
                >
                  <ListOrdered size={16} />
                </button>
                <div className="w-px h-6 bg-gray-300 mx-1"></div>

                {/* Text Color */}
                <div className="relative color-picker-container">
                  <button
                    type="button"
                    onClick={() => {
                      setShowColorPicker(!showColorPicker);
                      setShowHighlightPicker(false);
                    }}
                    className="p-2 hover:bg-gray-200 rounded transition-colors"
                    title="Text Color"
                  >
                    <Palette size={16} />
                  </button>
                  {showColorPicker && (
                    <div className="absolute top-full left-0 mt-1 z-50">
                      <div onClick={(e) => e.stopPropagation()}>
                        <SketchPicker
                          color={colorPickerColor}
                          onChange={(color) => {
                            setColorPickerColor(color);
                            formatText('foreColor', color.hex);
                          }}
                          onChangeComplete={(color) => {
                            setColorPickerColor(color);
                          }}
                        />
                      </div>
                    </div>
                  )}
                </div>

                {/* Highlight */}
                <div className="relative highlight-picker-container">
                  <button
                    type="button"
                    onClick={() => {
                      setShowHighlightPicker(!showHighlightPicker);
                      setShowColorPicker(false);
                    }}
                    className="p-2 hover:bg-gray-200 rounded transition-colors"
                    title="Highlight"
                  >
                    <Highlighter size={16} />
                  </button>
                  {showHighlightPicker && (
                    <div className="absolute top-full left-0 mt-1 z-50">
                      <div onClick={(e) => e.stopPropagation()}>
                        <SketchPicker
                          color={highlightPickerColor}
                          onChange={(color) => {
                            setHighlightPickerColor(color);
                            formatText('backColor', color.hex);
                          }}
                          onChangeComplete={(color) => {
                            setHighlightPickerColor(color);
                          }}
                        />
                      </div>
                    </div>
                  )}
                </div>

                <div className="w-px h-6 bg-gray-300 mx-1"></div>

                {/* Font Size */}
                <select
                  onChange={(e) => {
                    if (e.target.value) {
                      formatText('fontSize', e.target.value);
                      e.target.value = '';
                    }
                  }}
                  className="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
                  title="Font Size"
                >
                  <option value="">Size</option>
                  <option value="1">8pt</option>
                  <option value="2">10pt</option>
                  <option value="3">12pt</option>
                  <option value="4">14pt</option>
                  <option value="5">18pt</option>
                  <option value="6">24pt</option>
                  <option value="7">36pt</option>
                </select>

                {/* Headings */}
                <select
                  onChange={(e) => {
                    if (e.target.value) {
                      formatText('formatBlock', e.target.value);
                      e.target.value = '';
                    }
                  }}
                  className="px-2 py-1 text-xs border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-blue-500"
                  title="Heading"
                >
                  <option value="">Heading</option>
                  <option value="h1">Heading 1</option>
                  <option value="h2">Heading 2</option>
                  <option value="h3">Heading 3</option>
                  <option value="p">Normal</option>
                </select>

                <div className="w-px h-6 bg-gray-300 mx-1"></div>

                {/* Variables */}
                <div className="relative">
                  <button
                    type="button"
                    onClick={() => {
                      setShowVariables(!showVariables);
                      setShowColorPicker(false);
                      setShowHighlightPicker(false);
                    }}
                    className="px-2 py-1 text-xs border border-gray-300 rounded hover:bg-gray-200 transition-colors flex items-center gap-1"
                    title="Insert Variable"
                  >
                    <Type size={14} />
                    Variables
                  </button>
                  {showVariables && (
                    <div className="absolute top-full left-0 mt-1 bg-white border border-gray-200 rounded-lg shadow-lg z-50 w-64 max-h-80 overflow-y-auto">
                      <div className="p-2 border-b border-gray-200">
                        <p className="text-xs font-semibold text-gray-700">Insert Variable</p>
                      </div>
                      <div className="p-1">
                        {AVAILABLE_VARIABLES.map(variable => {
                          const Icon = variable.icon;
                          return (
                            <button
                              key={variable.key}
                              type="button"
                              onClick={() => insertVariable(variable.key)}
                              className="w-full flex items-center gap-2 px-2 py-1.5 text-xs text-left hover:bg-gray-100 rounded transition-colors"
                            >
                              <Icon size={14} className="text-gray-500" />
                              <span>{variable.label}</span>
                              <span className="ml-auto text-gray-400 font-mono">{`{{${variable.key}}}`}</span>
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>

                {/* Link */}
                <button
                  type="button"
                  onClick={insertLink}
                  className="p-2 hover:bg-gray-200 rounded transition-colors"
                  title="Insert Link"
                >
                  <Link size={16} />
                </button>

                {/* Signature */}
                <button
                  type="button"
                  onClick={insertSignature}
                  className="px-2 py-1 text-xs border border-gray-300 rounded hover:bg-gray-200 transition-colors flex items-center gap-1"
                  title="Insert Signature"
                >
                  <FileText size={14} />
                  Signature
                </button>
              </div>
              
              {/* Editor */}
              <div className="relative">
                <div
                  ref={editorRef}
                  contentEditable
                  onInput={handleEditorChange}
                  onBlur={() => {
                    setIsEditorFocused(false);
                    handleEditorChange();
                  }}
                  onFocus={() => setIsEditorFocused(true)}
                  className={`min-h-[350px] p-4 text-sm text-gray-700 focus:outline-none ${
                    isEditorFocused ? '' : ''
                  }`}
                  style={{ minHeight: '350px' }}
                  data-placeholder="Compose your email here...

Use the Variables button to insert personalization variables like {{firstName}}, {{company}}, etc."
                />
                
                {/* AI Writing Assistant - Floating Button */}
                <div className="absolute bottom-4 right-4 z-10">
                  <button
                    type="button"
                    onClick={handleAIWriting}
                    disabled={isAIGenerating}
                    className="px-4 py-2.5 text-sm bg-white/80 backdrop-blur-md border border-purple-200/50 rounded-xl shadow-lg hover:shadow-xl transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-white/90 hover:border-purple-300/70 group"
                    title="AI Writing Assistant"
                  >
                    {isAIGenerating ? (
                      <>
                        <Loader size={16} className="animate-spin text-purple-600" />
                        <span className="text-gray-700 font-medium">Generating...</span>
                      </>
                    ) : (
                      <>
                        <Sparkles size={16} className="text-purple-600 group-hover:scale-110 transition-transform" />
                        <span className="text-gray-700 font-medium">AI Writing</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          </>
        ) : (
          /* Preview Mode */
          <div className="border border-gray-200 rounded min-h-[400px] bg-gray-50 p-6">
            {previewMode === 'desktop' && (
              <div className="flex flex-col items-center gap-4">
                {/* Email Client Selector - Matching iPhone Notification Selector Style */}
                <div className="flex items-center gap-2 bg-white rounded-lg border border-gray-200 p-1">
                  <button
                    type="button"
                    onClick={() => setEmailClient('gmail')}
                    className={`px-3 py-1.5 text-xs rounded transition-colors ${
                      emailClient === 'gmail'
                        ? 'bg-red-600 text-white'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                    title="Gmail"
                  >
                    Gmail
                  </button>
                  <button
                    type="button"
                    onClick={() => setEmailClient('outlook')}
                    className={`px-3 py-1.5 text-xs rounded transition-colors ${
                      emailClient === 'outlook'
                        ? 'bg-blue-600 text-white'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                    title="Outlook"
                  >
                    Outlook
                  </button>
                  <button
                    type="button"
                    onClick={() => setEmailClient('apple-mail')}
                    className={`px-3 py-1.5 text-xs rounded transition-colors ${
                      emailClient === 'apple-mail'
                        ? 'bg-gray-700 text-white'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                    title="Apple Mail"
                  >
                    Apple Mail
                  </button>
                </div>

                {/* Email Client Preview */}
                {emailClient === 'gmail' && (
                  /* Gmail Inbox Preview */
                  <div className="w-full max-w-4xl bg-white rounded-lg shadow-lg overflow-hidden border border-gray-200">
                    {/* Gmail Header */}
                    <div className="bg-white border-b border-gray-300 px-4 py-3 flex items-center justify-between" style={{ backgroundColor: '#f8f9fa' }}>
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-red-500 to-red-600 flex items-center justify-center text-white font-semibold text-sm">
                          G
                        </div>
                        <div>
                          <h3 className="text-sm font-medium text-gray-900" style={{ fontFamily: 'Google Sans, Roboto, sans-serif' }}>Gmail</h3>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center">
                          <span className="text-xs text-gray-600">1</span>
                        </div>
                      </div>
                    </div>

                    {/* Gmail Tabs */}
                    <div className="bg-white border-b border-gray-200 px-4">
                      <div className="flex items-center gap-1">
                        <button className="px-4 py-2 text-sm font-medium text-blue-600 border-b-2 border-blue-600" style={{ fontFamily: 'Google Sans, Roboto, sans-serif' }}>
                          Primary
                        </button>
                        <button className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900" style={{ fontFamily: 'Google Sans, Roboto, sans-serif' }}>
                          Promotions
                        </button>
                        <button className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900" style={{ fontFamily: 'Google Sans, Roboto, sans-serif' }}>
                          Social
                        </button>
                      </div>
                    </div>

                    {/* Gmail Email List */}
                    <div className="bg-white">
                      <div className="border-b border-gray-200 hover:bg-gray-50 transition-colors cursor-pointer" style={{ backgroundColor: '#fff' }}>
                        <div className="px-4 py-3 flex items-start gap-3">
                          <div className="flex items-center gap-2 flex-shrink-0">
                            <input type="checkbox" className="w-4 h-4 rounded border-gray-300" />
                            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center flex-shrink-0">
                              <span className="text-white text-sm font-semibold">Y</span>
                            </div>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-1">
                              <div className="flex items-center gap-2 flex-1 min-w-0">
                                <span className="text-sm font-semibold text-gray-900 flex-shrink-0" style={{ fontFamily: 'Google Sans, Roboto, sans-serif' }}>Your Email</span>
                                <span className="text-xs text-gray-500 flex-shrink-0">-</span>
                                <span className="text-xs text-gray-600 truncate flex-1">
                                  {(getPreviewContent().replace(/<[^>]*>/g, '').substring(0, 60) || 'No content yet') + (getPreviewContent().replace(/<[^>]*>/g, '').length > 60 ? '...' : '')}
                                </span>
                              </div>
                              <span className="text-xs text-gray-500 flex-shrink-0 ml-2">Just now</span>
                            </div>
                            <p className="text-sm font-medium text-gray-900" style={{ fontFamily: 'Google Sans, Roboto, sans-serif' }}>
                              {getPreviewSubject() || '(No subject)'}
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {emailClient === 'outlook' && (
                  /* Outlook Inbox Preview */
                  <div className="w-full max-w-4xl bg-white rounded-lg shadow-lg overflow-hidden border border-gray-200">
                    {/* Outlook Header */}
                    <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between" style={{ backgroundColor: '#0078d4' }}>
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded flex items-center justify-center bg-white">
                          <svg width="20" height="20" viewBox="0 0 24 24" fill="#0078d4">
                            <path d="M7 4h10v2H7V4zm0 4h10v2H7V8zm0 4h7v2H7v-2zm-4 6h18v2H3v-2z"/>
                          </svg>
                        </div>
                        <h3 className="text-sm font-semibold text-white" style={{ fontFamily: 'Segoe UI, sans-serif' }}>Outlook</h3>
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded bg-white/20 flex items-center justify-center">
                          <span className="text-xs text-white">1</span>
                        </div>
                      </div>
                    </div>

                    {/* Outlook Tabs */}
                    <div className="bg-white border-b border-gray-200 px-4">
                      <div className="flex items-center gap-1">
                        <button className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900" style={{ fontFamily: 'Segoe UI, sans-serif' }}>
                          Focused
                        </button>
                        <button className="px-4 py-2 text-sm font-medium text-gray-900 border-b-2 border-gray-900" style={{ fontFamily: 'Segoe UI, sans-serif' }}>
                          Other
                        </button>
                      </div>
                    </div>

                    {/* Outlook Email List */}
                    <div className="bg-white">
                      <div className="border-b border-gray-200 hover:bg-blue-50 transition-colors cursor-pointer" style={{ backgroundColor: '#fff' }}>
                        <div className="px-4 py-3 flex items-start gap-3">
                          <div className="flex items-center gap-2 flex-shrink-0">
                            <input type="checkbox" className="w-4 h-4 rounded border-gray-300" />
                            <div className="w-10 h-10 rounded-full bg-blue-600 flex items-center justify-center flex-shrink-0">
                              <span className="text-white text-sm font-semibold">Y</span>
                            </div>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-1">
                              <div className="flex items-center gap-2 flex-1 min-w-0">
                                <span className="text-sm font-semibold text-gray-900 flex-shrink-0" style={{ fontFamily: 'Segoe UI, sans-serif' }}>Your Email</span>
                                <span className="text-xs text-gray-500 flex-shrink-0">•</span>
                                <span className="text-xs text-gray-600 truncate flex-1" style={{ fontFamily: 'Segoe UI, sans-serif' }}>
                                  {(getPreviewContent().replace(/<[^>]*>/g, '').substring(0, 50) || 'No content yet') + (getPreviewContent().replace(/<[^>]*>/g, '').length > 50 ? '...' : '')}
                                </span>
                              </div>
                              <span className="text-xs text-gray-500 flex-shrink-0 ml-2" style={{ fontFamily: 'Segoe UI, sans-serif' }}>Just now</span>
                            </div>
                            <p className="text-sm font-semibold text-gray-900" style={{ fontFamily: 'Segoe UI, sans-serif' }}>
                              {getPreviewSubject() || '(No subject)'}
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {emailClient === 'apple-mail' && (
                  /* Apple Mail Inbox Preview */
                  <div className="w-full max-w-4xl bg-white rounded-lg shadow-lg overflow-hidden border border-gray-200">
                    {/* Apple Mail Header */}
                    <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center justify-between" style={{ backgroundColor: '#f5f5f7' }}>
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
                          <Mail size={18} className="text-white" />
                        </div>
                        <h3 className="text-sm font-semibold text-gray-900" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display", sans-serif' }}>Mail</h3>
                        <span className="text-xs text-gray-500">Inbox</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center">
                          <span className="text-xs text-white font-semibold">1</span>
                        </div>
                      </div>
                    </div>

                    {/* Apple Mail Email List */}
                    <div className="bg-white">
                      <div className="border-b border-gray-200 hover:bg-gray-50 transition-colors cursor-pointer" style={{ backgroundColor: '#fff' }}>
                        <div className="px-4 py-3 flex items-start gap-3">
                          <div className="flex items-center gap-2 flex-shrink-0">
                            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center flex-shrink-0">
                              <span className="text-white text-sm font-semibold">Y</span>
                            </div>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-1">
                              <div className="flex items-center gap-2 flex-1 min-w-0">
                                <span className="text-sm font-semibold text-gray-900 flex-shrink-0" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif' }}>Your Email</span>
                                <span className="text-xs text-gray-500 flex-shrink-0">•</span>
                                <span className="text-xs text-gray-600 truncate flex-1" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif' }}>
                                  {(getPreviewContent().replace(/<[^>]*>/g, '').substring(0, 55) || 'No content yet') + (getPreviewContent().replace(/<[^>]*>/g, '').length > 55 ? '...' : '')}
                                </span>
                              </div>
                              <span className="text-xs text-gray-500 flex-shrink-0 ml-2" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif' }}>Just now</span>
                            </div>
                            <p className="text-sm font-semibold text-gray-900" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif' }}>
                              {getPreviewSubject() || '(No subject)'}
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {previewMode === 'phone' && (
              <div className="flex flex-col items-center gap-4">
                {/* Phone Notification Type Selector */}
                <div className="flex items-center gap-2 bg-white rounded-lg border border-gray-200 p-1">
                  <button
                    type="button"
                    onClick={() => setPhoneNotificationType('lockscreen')}
                    className={`px-3 py-1.5 text-xs rounded transition-colors ${
                      phoneNotificationType === 'lockscreen'
                        ? 'bg-blue-600 text-white'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                  >
                    Lock Screen
                  </button>
                  <button
                    type="button"
                    onClick={() => setPhoneNotificationType('popup')}
                    className={`px-3 py-1.5 text-xs rounded transition-colors ${
                      phoneNotificationType === 'popup'
                        ? 'bg-blue-600 text-white'
                        : 'text-gray-600 hover:bg-gray-100'
                    }`}
                  >
                    Banner Notification
                  </button>
                </div>

                {/* iPhone Frame - Realistic Design */}
                <div className="w-[280px] bg-black rounded-[2.25rem] p-1 shadow-2xl" style={{ boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)' }}>
                  <div className="bg-white rounded-[2rem] overflow-hidden relative" style={{ height: '606px' }}>
                    {/* iOS Status Bar */}
                    <div className="bg-black text-white text-[8px] px-4 pt-0.5 pb-0 flex justify-between items-center font-medium" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", sans-serif' }}>
                      <div className="flex items-center gap-1">
                        <span>9:41</span>
                      </div>
                      <div className="flex items-center gap-0.5">
                        <svg width="13" height="8" viewBox="0 0 17 10" fill="none">
                          <rect x="0.5" y="0.5" width="16" height="9" rx="1" stroke="white" strokeWidth="1"/>
                          <rect x="2" y="2" width="13" height="6" fill="white"/>
                        </svg>
                        <svg width="11" height="8" viewBox="0 0 15 11" fill="none">
                          <path d="M1 5.5L3 3.5L5.5 6L9.5 2L12 4.5L14 2.5" stroke="white" strokeWidth="1.5" strokeLinecap="round"/>
                          <circle cx="7.5" cy="5.5" r="1" fill="white"/>
                        </svg>
                        <div className="w-3 h-1.5 border border-white rounded-sm">
                          <div className="w-2 h-1 bg-white rounded-sm m-0.5"></div>
                        </div>
                      </div>
                    </div>

                    {phoneNotificationType === 'lockscreen' ? (
                      /* iOS Lock Screen Notification */
                      <>
                        {/* Lock Screen Background - Realistic iOS Wallpaper */}
                        <div className="relative h-full bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-500" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)' }}>
                          {/* Dynamic Island (for iPhone 14 Pro+) */}
                          <div className="absolute top-1.5 left-1/2 transform -translate-x-1/2 w-24 h-5 bg-black rounded-full z-20"></div>
                          
                          {/* Lock Screen Time - iOS Style */}
                          <div className="absolute top-18 left-0 right-0 text-center text-white" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display", sans-serif' }}>
                            <div className="text-[60px] font-thin leading-none mb-1.5" style={{ fontWeight: '100', letterSpacing: '-2px' }}>9:41</div>
                            <div className="text-[15px] font-medium" style={{ fontWeight: '500' }}>Monday, November 7</div>
                          </div>

                          {/* iOS Lock Screen Notification - Realistic Design */}
                          <div className="absolute bottom-20 left-2 right-2 z-30">
                            <div 
                              className="rounded-[10px] overflow-hidden backdrop-blur-xl"
                              style={{
                                background: 'rgba(255, 255, 255, 0.85)',
                                backdropFilter: 'blur(40px) saturate(180%)',
                                WebkitBackdropFilter: 'blur(40px) saturate(180%)',
                                boxShadow: '0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08)',
                                border: '0.5px solid rgba(255, 255, 255, 0.3)'
                              }}
                            >
                              <div className="px-3 py-2.5">
                                <div className="flex items-start gap-2.5">
                                  {/* App Icon */}
                                  <div className="w-9 h-9 rounded-[8px] bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center flex-shrink-0 shadow-sm" style={{ boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)' }}>
                                    <Mail size={18} className="text-white" />
                                  </div>
                                  <div className="flex-1 min-w-0 pt-0.5">
                                    <div className="flex items-center justify-between mb-0.5">
                                      <span className="text-[11px] font-semibold text-gray-900" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif', fontWeight: '600' }}>Mail</span>
                                      <span className="text-[9px] text-gray-500" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif' }}>now</span>
                                    </div>
                                    <p className="text-[12px] font-semibold text-gray-900 mb-0.5 leading-tight" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif', fontWeight: '600' }}>
                                      {getPreviewSubject() || 'New Email'}
                                    </p>
                                    <p className="text-[10px] text-gray-600 leading-snug line-clamp-2" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif' }}>
                                      {(getPreviewContent().replace(/<[^>]*>/g, '').substring(0, 65) || 'No content yet') + (getPreviewContent().replace(/<[^>]*>/g, '').length > 65 ? '...' : '')}
                                    </p>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>

                          {/* Lock Indicator - iOS Style */}
                          <div className="absolute bottom-9 left-0 right-0 flex justify-center">
                            <div className="w-0.5 h-6 bg-white/40 rounded-full" style={{ opacity: '0.4' }}></div>
                          </div>
                        </div>
                      </>
                    ) : (
                      /* iOS Banner Notification - Realistic Design */
                      <>
                        {/* App Content Background */}
                        <div className="bg-gray-50 min-h-full relative" style={{ backgroundColor: '#f2f2f7' }}>
                          {/* iOS Banner Notification from Top - Realistic Design */}
                          <div className="absolute top-0 left-0 right-0 z-50 animate-slide-down px-2 pt-1.5">
                            <div 
                              className="rounded-[10px] overflow-hidden backdrop-blur-xl"
                              style={{
                                background: 'rgba(255, 255, 255, 0.95)',
                                backdropFilter: 'blur(40px) saturate(180%)',
                                WebkitBackdropFilter: 'blur(40px) saturate(180%)',
                                boxShadow: '0 8px 32px rgba(0, 0, 0, 0.15), 0 2px 8px rgba(0, 0, 0, 0.1)',
                                border: '0.5px solid rgba(0, 0, 0, 0.1)'
                              }}
                            >
                              <div className="px-3 py-2">
                                <div className="flex items-start gap-2.5">
                                  {/* App Icon */}
                                  <div className="w-8 h-8 rounded-[7px] bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center flex-shrink-0 shadow-sm" style={{ boxShadow: '0 2px 6px rgba(0, 0, 0, 0.12)' }}>
                                    <Mail size={16} className="text-white" />
                                  </div>
                                  <div className="flex-1 min-w-0 pt-0.5">
                                    <div className="flex items-center justify-between mb-0.5">
                                      <span className="text-[11px] font-semibold text-gray-900" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif', fontWeight: '600' }}>Mail</span>
                                      <button className="text-gray-400 hover:text-gray-600 transition-colors p-0.5" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif' }}>
                                        <X size={12} />
                                      </button>
                                    </div>
                                    <p className="text-[12px] font-semibold text-gray-900 mb-0.5 truncate leading-tight" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif', fontWeight: '600' }}>
                                      {getPreviewSubject() || 'New Email'}
                                    </p>
                                    <p className="text-[10px] text-gray-600 leading-snug line-clamp-2" style={{ fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", sans-serif' }}>
                                      {(getPreviewContent().replace(/<[^>]*>/g, '').substring(0, 50) || 'No content yet') + (getPreviewContent().replace(/<[^>]*>/g, '').length > 50 ? '...' : '')}
                                    </p>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>

                          {/* App Content (simulated iOS app) */}
                          <div className="pt-18 px-3 pb-3">
                            <div className="bg-white rounded-[8px] shadow-sm p-3 mb-2" style={{ backgroundColor: '#ffffff', borderRadius: '8px' }}>
                              <div className="h-3 bg-gray-200 rounded w-3/4 mb-2" style={{ backgroundColor: '#e5e5ea', borderRadius: '4px' }}></div>
                            </div>
                            <div className="bg-white rounded-[8px] shadow-sm p-3" style={{ backgroundColor: '#ffffff', borderRadius: '8px' }}>
                              <div className="h-3 bg-gray-200 rounded w-1/2 mb-1.5" style={{ backgroundColor: '#e5e5ea', borderRadius: '4px' }}></div>
                              <div className="h-3 bg-gray-200 rounded w-full" style={{ backgroundColor: '#e5e5ea', borderRadius: '4px' }}></div>
                            </div>
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {!emailContent || emailContent === '<p><br></p>' || emailContent.trim() === '' || emailContent === '<div><br></div>' ? (
          <p className="text-red-500 text-xs mt-1">
            * Email content is required
          </p>
        ) : null}

        {/* AI Feedback Panel */}
        {showAIFeedback && (aiFeedback.subject.length > 0 || aiFeedback.content.length > 0 || isAIGenerating) && (
          <div className="mt-3 border border-purple-200 rounded-lg bg-gradient-to-br from-purple-50 to-blue-50 p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Sparkles size={16} className="text-purple-600" />
                <h4 className="text-sm font-semibold text-gray-800">AI Writing Assistant</h4>
                {isAIGenerating && (
                  <Loader size={14} className="animate-spin text-purple-600" />
                )}
              </div>
              <button
                onClick={() => setShowAIFeedback(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X size={16} />
              </button>
            </div>
            
            <div className="space-y-3">
              {/* Subject Feedback */}
              {aiFeedback.subject.length > 0 && (
                <div>
                  <p className="text-xs font-medium text-gray-700 mb-1.5">Subject Line:</p>
                  <div className="space-y-1.5">
                    {aiFeedback.subject.map((feedback, idx) => (
                      <div
                        key={idx}
                        className={`p-2 rounded text-xs flex items-start gap-2 ${
                          feedback.type === 'warning' 
                            ? 'bg-yellow-50 border border-yellow-200 text-yellow-800' 
                            : 'bg-blue-50 border border-blue-200 text-blue-800'
                        }`}
                      >
                        {feedback.type === 'warning' ? (
                          <AlertCircle size={14} className="mt-0.5 flex-shrink-0" />
                        ) : (
                          <Info size={14} className="mt-0.5 flex-shrink-0" />
                        )}
                        <div className="flex-1">
                          <p>{feedback.text}</p>
                          {feedback.suggestion && (
                            <p className="mt-1 font-medium">Suggestion: "{feedback.suggestion}"</p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Content Feedback */}
              {aiFeedback.content.length > 0 && (
                <div>
                  <p className="text-xs font-medium text-gray-700 mb-1.5">Email Content:</p>
                  <div className="space-y-1.5">
                    {aiFeedback.content.map((feedback, idx) => (
                      <div
                        key={idx}
                        className={`p-2 rounded text-xs flex items-start gap-2 ${
                          feedback.type === 'warning' 
                            ? 'bg-yellow-50 border border-yellow-200 text-yellow-800' 
                            : 'bg-blue-50 border border-blue-200 text-blue-800'
                        }`}
                      >
                        {feedback.type === 'warning' ? (
                          <AlertCircle size={14} className="mt-0.5 flex-shrink-0" />
                        ) : (
                          <Info size={14} className="mt-0.5 flex-shrink-0" />
                        )}
                        <p className="flex-1">{feedback.text}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Show AI Feedback toggle if hidden */}
        {!showAIFeedback && (
          <button
            onClick={() => setShowAIFeedback(true)}
            className="mt-3 w-full px-3 py-2 text-xs bg-purple-50 hover:bg-purple-100 border border-purple-200 rounded-lg text-purple-700 flex items-center justify-center gap-2 transition-colors"
          >
            <Sparkles size={14} />
            Show AI Feedback
          </button>
        )}

        {/* Recommendations */}
        {recommendations.length > 0 && (
          <div className="mt-3 space-y-1">
            {recommendations.map((rec, idx) => (
              <div
                key={idx}
                className={`p-2 rounded text-xs flex items-start gap-2 ${
                  rec.type === 'warning' ? 'bg-yellow-50 border border-yellow-200 text-yellow-800' :
                  rec.type === 'success' ? 'bg-green-50 border border-green-200 text-green-800' :
                  'bg-blue-50 border border-blue-200 text-blue-800'
                }`}
              >
                {rec.type === 'warning' && <AlertCircle size={14} className="mt-0.5 flex-shrink-0" />}
                {rec.type === 'success' && <CheckCircle size={14} className="mt-0.5 flex-shrink-0" />}
                {rec.type === 'info' && <Info size={14} className="mt-0.5 flex-shrink-0" />}
                <span>{rec.text}</span>
              </div>
            ))}
          </div>
        )}

        {/* Personalization Tips */}
        <div className="mt-3 p-3 bg-blue-50 rounded border border-blue-200">
          <p className="text-xs text-blue-800 font-medium mb-1">
            💡 Cold Outreach Best Practices:
          </p>
          <ul className="text-xs text-blue-700 space-y-0.5 list-disc list-inside">
            <li>Keep emails under 100 words for better response rates</li>
            <li>Use personalization variables to make emails feel personalized</li>
            <li>Subject lines should be under 50 characters</li>
            <li>Focus on the prospect's pain point, not your product</li>
            <li>Include a clear, single call-to-action</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
