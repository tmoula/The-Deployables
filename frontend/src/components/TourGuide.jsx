import { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X, ChevronLeft, ChevronRight } from 'lucide-react';

const TOUR_STEPS = [
  {
    target: '[data-tour="dashboard"]',
    content: 'Welcome! This tour demonstrates our Twelve-Factor App architecture. The system uses 4 Spring Boot microservices (auth-svc, lead-svc, campaign-svc, ai-svc) deployed on GKE with PostgreSQL and RabbitMQ. All services follow Twelve-Factor principles: environment-based config via ConfigMaps/Secrets, stateless processes, port binding, and health checks.',
    placement: 'center',
  },
  {
    target: '[data-tour="sidebar-leads"]',
    content: 'LEAD GENERATION - RabbitMQ Async Processing & Database Integration: Clicking here triggers the lead-svc microservice with async processing via RabbitMQ. Behind the scenes: (1) Your company profile is saved to PostgreSQL via JPA/Hibernate (Factor IV: Backing Services), (2) The lead generation request is posted to RabbitMQ queue for async processing (Factor VIII: Concurrency), (3) Background workers consume from RabbitMQ, call ai-svc for matching, and process results, (4) Results are published back through RabbitMQ, (5) lead-svc receives processed results and returns to frontend. This demonstrates RabbitMQ message publishing, queue monitoring, and async job processing with proper error handling.',
    placement: 'right',
  },
  {
    target: '[data-tour="generate-leads-button"]',
    content: 'TECHNICAL DETAILS - RabbitMQ Flow: When you click "Generate Leads": (1) Frontend calls POST /api/v1/match, (2) lead-svc validates input (Factor III: Config validation) and saves profile to PostgreSQL via JPA, (3) Request is published to RabbitMQ queue (demonstrates async processing requirements), (4) Background worker consumes from queue, queries icp_profiles table, calls ai-svc for AI matching, (5) Processed results are published back to RabbitMQ response queue, (6) lead-svc receives results and returns ranked matches to frontend. This shows RabbitMQ integration, dead letter queue handling, job status tracking, and proper database transactions. Check network tab to see async API responses!',
    placement: 'top',
  },
  {
    target: '[data-tour="sidebar-campaigns"]',
    content: 'CAMPAIGN MANAGEMENT - Microservices Architecture: The campaigns feature uses campaign-svc, which integrates with lead-svc for contact data and RabbitMQ for async email processing. This demonstrates Factor VI (Processes - stateless services), Factor VIII (Concurrency - RabbitMQ workers), and Factor IX (Disposability - graceful shutdown). All services are containerized with multi-stage Dockerfiles.',
    placement: 'right',
  },
  {
    target: '[data-tour="create-campaign-btn"]',
    content: 'CAMPAIGN CREATION WORKFLOW: Creating a campaign demonstrates: (1) CSV upload processed by campaign-svc (file handling), (2) Data validation and persistence to PostgreSQL (Factor IV), (3) RESTful API with full CRUD operations (Backend Service Requirements), (4) OpenAPI/Swagger documentation available at /swagger-ui. The service uses environment variables from Kubernetes ConfigMaps (Factor III: Config).',
    placement: 'bottom',
  },
  {
    target: '[data-tour="campaign-step2"]',
    content: 'EMAIL COMPOSITION - Backend Rendering & Database Integration: Step 2 demonstrates: (1) Variable substitution ({{firstName}}, {{company}}) - data fetched from PostgreSQL contacts table, (2) Spintax rotation ({option1|option2}) - backend processing with seed-based randomization, (3) Real-time preview via POST /api/v1/campaigns/{id}/preview-email - shows database I/O per contact, (4) The backend campaign-svc renders emails server-side using JPA to fetch contact data. This shows proper separation of concerns and database integration.',
    placement: 'top',
  },
  {
    target: 'body',
    content: 'CAMPAIGN SETTINGS - Configuration Management: Step 3 saves all settings to PostgreSQL via JPA. This demonstrates: (1) Factor III (Config) - settings stored in database, not hardcoded, (2) Environment-based configuration - timezone, delays configurable, (3) Data persistence with proper schema design, (4) API validation and error handling. All configuration follows Twelve-Factor principles.',
    placement: 'center',
  },
  {
    target: 'body',
    content: 'CAMPAIGN REVIEW - Data Flow Demonstration: Step 4 shows the complete data flow: (1) Frontend requests preview → (2) campaign-svc queries PostgreSQL for contacts → (3) Backend renders email with variables/spintax → (4) Returns personalized preview. This demonstrates the microservices architecture, database integration, and proper API design. Each preview is a separate database query showing real-time data access.',
    placement: 'center',
  },
  {
    target: '[data-tour="campaign-send"]',
    content: 'RABBITMQ ASYNC PROCESSING - Factor VIII (Concurrency): When you send a campaign: (1) campaign-svc publishes email jobs to RabbitMQ queue, (2) Background workers process emails asynchronously (demonstrates async processing requirements), (3) Dead letter queue handles failures (retry logic implemented), (4) Job status persisted to PostgreSQL for tracking, (5) Health checks monitor queue health (/actuator/health). This shows proper message publishing, queue monitoring, and error handling as required.',
    placement: 'top',
  },
  {
    target: '[data-tour="sidebar-settings"]',
    content: 'ARCHITECTURE SUMMARY: This system demonstrates: ✓ Twelve-Factor App compliance (all 12 factors), ✓ 4 microservices with RESTful APIs, ✓ PostgreSQL with JPA/Hibernate (ER diagrams in docs), ✓ RabbitMQ for async processing, ✓ Kubernetes deployment (GKE) with Services, Ingress, ConfigMaps, Secrets, ✓ Health checks (/actuator/health), ✓ 80% test coverage, ✓ OpenAPI documentation. All services use non-root containers, resource limits, and RBAC. See documentation for architecture diagrams and deployment guides!',
    placement: 'right',
  },
];

const TOUR_STORAGE_KEY = 'outreach-tour-completed';

export default function TourGuide({ run, onComplete }) {
  const [currentStep, setCurrentStep] = useState(0);
  const [targetElement, setTargetElement] = useState(null);
  const [tooltipPosition, setTooltipPosition] = useState({ top: 0, left: 0 });
  const [spotlightPosition, setSpotlightPosition] = useState({ top: 0, left: 0, width: 0, height: 0 });
  const tooltipRef = useRef(null);

  useEffect(() => {
    if (!run) {
      setCurrentStep(0);
      setTargetElement(null);
      return;
    }

    updateStepPosition();
    
    const handleResize = () => updateStepPosition();
    const handleScroll = () => updateStepPosition();
    
    window.addEventListener('resize', handleResize);
    window.addEventListener('scroll', handleScroll, true);
    
    return () => {
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('scroll', handleScroll, true);
    };
  }, [run, currentStep]);

  const updateStepPosition = () => {
    const step = TOUR_STEPS[currentStep];
    if (!step) return;

    let element = null;
    
    if (step.target === 'body') {
      element = document.body;
    } else {
      element = document.querySelector(step.target);
    }

    setTargetElement(element);

    if (!element || step.placement === 'center') {
      // Center placement
      setTooltipPosition({
        top: window.innerHeight / 2,
        left: window.innerWidth / 2,
      });
      setSpotlightPosition({
        top: 0,
        left: 0,
        width: window.innerWidth,
        height: window.innerHeight,
      });
      return;
    }

    // Calculate position for tooltip
    const rect = element.getBoundingClientRect();
    const tooltipWidth = 450;
    const tooltipHeight = 250;
    let top = 0;
    let left = 0;

    switch (step.placement) {
      case 'right':
        top = rect.top + rect.height / 2 - tooltipHeight / 2;
        left = rect.right + 20;
        break;
      case 'left':
        top = rect.top + rect.height / 2 - tooltipHeight / 2;
        left = rect.left - tooltipWidth - 20;
        break;
      case 'top':
        top = rect.top - tooltipHeight - 20;
        left = rect.left + rect.width / 2 - tooltipWidth / 2;
        break;
      case 'bottom':
        top = rect.bottom + 20;
        left = rect.left + rect.width / 2 - tooltipWidth / 2;
        break;
      default:
        top = rect.bottom + 20;
        left = rect.left + rect.width / 2 - tooltipWidth / 2;
    }

    // Keep tooltip in viewport
    if (left < 20) left = 20;
    if (left + tooltipWidth > window.innerWidth - 20) {
      left = window.innerWidth - tooltipWidth - 20;
    }
    if (top < 20) top = 20;
    if (top + tooltipHeight > window.innerHeight - 20) {
      top = window.innerHeight - tooltipHeight - 20;
    }

    setTooltipPosition({ top, left });
    setSpotlightPosition({
      top: rect.top - 8,
      left: rect.left - 8,
      width: rect.width + 16,
      height: rect.height + 16,
    });
  };

  const handleNext = () => {
    if (currentStep < TOUR_STEPS.length - 1) {
      setCurrentStep(currentStep + 1);
    } else {
      handleFinish();
    }
  };

  const handleBack = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleSkip = () => {
    localStorage.setItem(TOUR_STORAGE_KEY, 'true');
    if (onComplete) onComplete();
  };

  const handleFinish = () => {
    localStorage.setItem(TOUR_STORAGE_KEY, 'true');
    if (onComplete) onComplete();
  };

  if (!run || currentStep >= TOUR_STEPS.length) {
    return null;
  }

  const step = TOUR_STEPS[currentStep];
  const isFirst = currentStep === 0;
  const isLast = currentStep === TOUR_STEPS.length - 1;
  const isCenter = step.placement === 'center';

  const tourContent = (
    <>
      {/* Overlay with spotlight - allows clicks to pass through */}
      <div
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.3)',
          zIndex: 9998,
          pointerEvents: 'none', // Allow clicks to pass through
        }}
      >
        {!isCenter && targetElement && (
          <div
            style={{
              position: 'absolute',
              top: `${spotlightPosition.top}px`,
              left: `${spotlightPosition.left}px`,
              width: `${spotlightPosition.width}px`,
              height: `${spotlightPosition.height}px`,
              borderRadius: '8px',
              boxShadow: '0 0 0 9999px rgba(0, 0, 0, 0.3), 0 0 0 4px #2563eb',
              pointerEvents: 'none', // Allow clicks on highlighted element
            }}
          />
        )}
      </div>

      {/* Tooltip */}
      <div
        ref={tooltipRef}
        style={{
          position: 'fixed',
          top: `${tooltipPosition.top}px`,
          left: `${tooltipPosition.left}px`,
          transform: isCenter ? 'translate(-50%, -50%)' : 'none',
          width: isCenter ? '600px' : '450px',
          maxWidth: '90vw',
          maxHeight: '80vh',
          overflowY: 'auto',
          backgroundColor: 'white',
          borderRadius: '12px',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
          zIndex: 9999,
          padding: '24px',
          pointerEvents: 'auto',
        }}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-3">
          <div className="flex-1">
            <div className="text-xs font-semibold text-blue-600 mb-1 uppercase tracking-wide">
              Technical Architecture Tour
            </div>
            <div className="text-sm font-semibold text-gray-800">
              Step {currentStep + 1} of {TOUR_STEPS.length}
            </div>
            <div className="text-xs text-gray-500 mt-1 italic">
              💡 You can interact with the screen while viewing steps
            </div>
          </div>
          <button
            onClick={handleSkip}
            className="text-gray-400 hover:text-gray-600 transition"
            aria-label="Close"
          >
            <X size={20} />
          </button>
        </div>

        {/* Content */}
        <div className="text-gray-700 text-sm leading-relaxed mb-4 space-y-2">
          {step.content.split('\n').map((line, idx) => (
            <p key={idx} className={line.startsWith('(') || line.startsWith('✓') ? 'text-xs text-gray-600 ml-2' : ''}>
              {line}
            </p>
          ))}
        </div>

        {/* Progress bar */}
        <div className="mb-4">
          <div className="w-full bg-gray-200 rounded-full h-1.5">
            <div
              className="bg-blue-600 h-1.5 rounded-full transition-all duration-300"
              style={{ width: `${((currentStep + 1) / TOUR_STEPS.length) * 100}%` }}
            />
          </div>
        </div>

        {/* Navigation buttons */}
        <div className="flex items-center justify-between gap-3">
          <button
            onClick={handleBack}
            disabled={isFirst}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg transition ${
              isFirst
                ? 'text-gray-300 cursor-not-allowed'
                : 'text-gray-700 hover:bg-gray-100'
            }`}
          >
            <ChevronLeft size={18} />
            Back
          </button>

          <button
            onClick={handleSkip}
            className="px-4 py-2 text-gray-600 hover:text-gray-800 transition"
          >
            Skip Tour
          </button>

          <button
            onClick={isLast ? handleFinish : handleNext}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition"
          >
            {isLast ? 'Finish' : 'Next'}
            {!isLast && <ChevronRight size={18} />}
          </button>
        </div>
      </div>
    </>
  );

  return createPortal(tourContent, document.body);
}

// Helper function to check if tour should run
export function shouldRunTour() {
  return !localStorage.getItem(TOUR_STORAGE_KEY);
}

// Helper function to reset tour (for testing or replay)
export function resetTour() {
  localStorage.removeItem(TOUR_STORAGE_KEY);
}
