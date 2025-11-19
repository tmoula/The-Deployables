import { useEffect, useRef } from "react";
import { Sparkles, Target, Zap, TrendingUp, Users, Mail, ArrowRight } from "lucide-react";

export default function Dashboard() {
  const heroRef = useRef(null);
  const featuresRef = useRef(null);
  const howItWorksRef = useRef(null);
  const teamRef = useRef(null);
  const ctaRef = useRef(null);

  useEffect(() => {
    const observerOptions = {
      threshold: 0.1,
      rootMargin: "0px 0px -100px 0px"
    };

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("animate-fade-in-up");
        }
      });
    }, observerOptions);

    const refs = [heroRef, featuresRef, howItWorksRef, teamRef, ctaRef];
    refs.forEach((ref) => {
      if (ref.current) {
        observer.observe(ref.current);
      }
    });

    return () => {
      refs.forEach((ref) => {
        if (ref.current) {
          observer.unobserve(ref.current);
        }
      });
    };
  }, []);

  const features = [
    {
      icon: Target,
      title: "Smart Lead Matching",
      description: "AI-powered matching algorithm finds the perfect prospects based on your company profile and criteria"
    },
    {
      icon: Zap,
      title: "Automated Discovery",
      description: "Discover potential enterprise clients that match your ideal customer profile automatically"
    },
    {
      icon: TrendingUp,
      title: "Scored Results",
      description: "Get ranked prospects with match scores to prioritize your outreach efforts"
    },
    {
      icon: Mail,
      title: "Campaign Management",
      description: "Manage your outreach campaigns and track engagement all in one place"
    }
  ];

  const teamMembers = [
    {
      name: "Daniel",
      role: "Developer",
      gradient: "from-blue-500 to-cyan-500"
    },
    {
      name: "Taha",
      role: "Developer",
      gradient: "from-purple-500 to-pink-500"
    },
    {
      name: "Francois",
      role: "Developer",
      gradient: "from-orange-500 to-red-500"
    }
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-blue-50 to-purple-50">
      {/* Hero Section */}
      <section 
        ref={heroRef}
        className="relative overflow-hidden opacity-0 transition-all duration-1000 ease-out"
        id="hero"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-blue-600/10 to-purple-600/10"></div>
        <div className="relative max-w-7xl mx-auto px-6 py-16 sm:py-24">
          <div className="text-center">
            <div className="flex justify-center mb-6">
              <div className="bg-gradient-to-r from-blue-600 to-purple-600 p-4 rounded-2xl shadow-lg">
                <Sparkles className="text-white" size={48} />
              </div>
            </div>
            <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-4">
              AI B2B Cold Outreach Agent
            </h1>
            <p className="text-xl md:text-2xl text-gray-600 mb-8 max-w-3xl mx-auto">
              Accelerate your B2B client acquisition with AI-powered lead discovery and personalized outreach
            </p>
            <div className="flex justify-center gap-4">
              <a
                href="/leads"
                className="bg-gradient-to-r from-blue-600 to-purple-600 text-white px-8 py-3 rounded-lg font-semibold hover:shadow-lg transition-all flex items-center gap-2"
              >
                Get Started
                <ArrowRight size={20} />
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section 
        ref={featuresRef}
        className="max-w-7xl mx-auto px-6 py-16 opacity-0 transition-all duration-1000 ease-out"
        id="features"
      >
        <div className="text-center mb-12">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
            Powerful Features
          </h2>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Everything you need to find, match, and reach out to your ideal customers
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((feature, idx) => (
            <div
              key={idx}
              className="bg-white rounded-xl shadow-md hover:shadow-xl transition-all duration-300 p-6 border border-gray-100 transform hover:-translate-y-1"
            >
              <div className="bg-gradient-to-br from-blue-500 to-purple-500 p-3 rounded-lg w-fit mb-4">
                <feature.icon className="text-white" size={24} />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                {feature.title}
              </h3>
              <p className="text-gray-600 text-sm">
                {feature.description}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* How It Works */}
      <section 
        ref={howItWorksRef}
        className="bg-white py-16 opacity-0 transition-all duration-1000 ease-out"
        id="how-it-works"
      >
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center mb-12">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
              How It Works
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="text-center transform transition-all duration-300 hover:scale-105">
              <div className="bg-gradient-to-br from-blue-500 to-cyan-500 rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4 shadow-lg">
                <span className="text-white text-2xl font-bold">1</span>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                Enter Your Company Info
              </h3>
              <p className="text-gray-600">
                Tell us about your company, industry, and value proposition
              </p>
            </div>

            <div className="text-center transform transition-all duration-300 hover:scale-105">
              <div className="bg-gradient-to-br from-purple-500 to-pink-500 rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4 shadow-lg">
                <span className="text-white text-2xl font-bold">2</span>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                Define Prospect Criteria
              </h3>
              <p className="text-gray-600">
                Specify what kind of companies you're looking for
              </p>
            </div>

            <div className="text-center transform transition-all duration-300 hover:scale-105">
              <div className="bg-gradient-to-br from-orange-500 to-red-500 rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4 shadow-lg">
                <span className="text-white text-2xl font-bold">3</span>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                Get Matched Leads
              </h3>
              <p className="text-gray-600">
                Receive scored prospects ready for outreach
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Team Section */}
      <section 
        ref={teamRef}
        className="max-w-7xl mx-auto px-6 py-16 opacity-0 transition-all duration-1000 ease-out"
        id="team"
      >
        <div className="text-center mb-12">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
            Built With ❤️ By
          </h2>
          <p className="text-lg text-gray-600">
            Meet the team behind this platform
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-4xl mx-auto">
          {teamMembers.map((member, idx) => (
            <div
              key={idx}
              className="bg-white rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 p-8 text-center border border-gray-100 transform hover:-translate-y-2"
            >
              <div className={`bg-gradient-to-br ${member.gradient} rounded-full w-24 h-24 flex items-center justify-center mx-auto mb-4 shadow-lg`}>
                <span className="text-white text-3xl font-bold">
                  {member.name.charAt(0)}
                </span>
              </div>
              <h3 className="text-2xl font-bold text-gray-900 mb-2">
                {member.name}
              </h3>
              <p className="text-gray-600 font-medium">
                {member.role}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA Section */}
      <section 
        ref={ctaRef}
        className="bg-gradient-to-r from-blue-600 to-purple-600 py-16 opacity-0 transition-all duration-1000 ease-out"
        id="cta"
      >
        <div className="max-w-4xl mx-auto px-6 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
            Ready to Find Your Next Client?
          </h2>
          <p className="text-xl text-blue-100 mb-8">
            Start generating leads and grow your business today
          </p>
          <a
            href="/leads"
            className="inline-flex items-center gap-2 bg-white text-blue-600 px-8 py-3 rounded-lg font-semibold hover:shadow-lg transition-all transform hover:scale-105"
          >
            Generate Leads
            <ArrowRight size={20} />
          </a>
        </div>
      </section>

      <style>{`
        @keyframes fadeInUp {
          from {
            opacity: 0;
            transform: translateY(30px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        
        .animate-fade-in-up {
          animation: fadeInUp 0.8s ease-out forwards;
        }
      `}</style>
    </div>
  );
}
