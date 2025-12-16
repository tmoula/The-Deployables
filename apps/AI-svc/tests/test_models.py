"""
Tests for Pydantic models
"""
import pytest
from pydantic import ValidationError
from src.models import (
    CompanyInfo, ContactInfo, EmailRequirements, SellerInfo,
    EmailGenerationRequest, EmailGenerationResponse,
    ProspectCriteria, ProspectInfo, SellerProfile
)


class TestCompanyInfo:
    """Tests for CompanyInfo model"""
    
    def test_valid_company_info(self):
        """Test creating a valid CompanyInfo"""
        company = CompanyInfo(
            company_id=1,
            name="Test Company",
            website="https://test.com",
            industry="Technology",
            employee_count=100,
            location="San Francisco",
            tech_stack=["Python", "AWS"],
            enrichment_notes="Growing startup"
        )
        assert company.name == "Test Company"
        assert company.company_id == 1
        assert company.employee_count == 100
    
    def test_company_info_minimal(self):
        """Test CompanyInfo with only required fields"""
        company = CompanyInfo(name="Minimal Company")
        assert company.name == "Minimal Company"
        assert company.company_id is None
        assert company.website is None
    
    def test_company_info_website_validation(self):
        """Test website URL validation"""
        # Valid URLs
        company1 = CompanyInfo(name="Test", website="https://example.com")
        assert company1.website == "https://example.com"
        
        # URL without protocol should be fixed
        company2 = CompanyInfo(name="Test", website="example.com")
        assert company2.website == "https://example.com"
    
    def test_company_info_invalid_employee_count(self):
        """Test that negative employee count is rejected"""
        with pytest.raises(ValidationError):
            CompanyInfo(name="Test", employee_count=-1)
    
    def test_company_info_name_too_long(self):
        """Test that name exceeding max length is rejected"""
        with pytest.raises(ValidationError):
            CompanyInfo(name="A" * 201)


class TestContactInfo:
    """Tests for ContactInfo model"""
    
    def test_valid_contact_info(self):
        """Test creating a valid ContactInfo"""
        contact = ContactInfo(
            contact_id=1,
            first_name="John",
            last_name="Doe",
            job_title="CEO",
            email="john@example.com",
            personalization_notes="Active on LinkedIn"
        )
        assert contact.first_name == "John"
        assert contact.email == "john@example.com"
    
    def test_contact_info_all_optional(self):
        """Test ContactInfo with all fields optional"""
        contact = ContactInfo()
        assert contact.contact_id is None
        assert contact.first_name is None
    
    def test_contact_info_valid_email(self):
        """Test valid email formats"""
        contact = ContactInfo(email="test@example.com")
        assert contact.email == "test@example.com"
    
    def test_contact_info_invalid_email(self):
        """Test that invalid email is rejected"""
        with pytest.raises(ValidationError):
            ContactInfo(email="invalid-email")
        
        with pytest.raises(ValidationError):
            ContactInfo(email="@example.com")


class TestEmailRequirements:
    """Tests for EmailRequirements model"""
    
    def test_valid_email_requirements(self):
        """Test creating valid EmailRequirements"""
        req = EmailRequirements(
            campaign_name="Q4 Campaign",
            product_service_description="AI-powered analytics platform for enterprises",
            value_proposition="Reduce costs by 30%",
            target_pain_points=["high costs", "manual processes"],
            tone="professional",
            email_length="medium",
            include_call_to_action=True
        )
        assert req.campaign_name == "Q4 Campaign"
        assert req.tone == "professional"
        assert req.include_call_to_action is True
    
    def test_email_requirements_minimal(self):
        """Test EmailRequirements with only required field"""
        req = EmailRequirements(
            product_service_description="A great product that solves problems"
        )
        assert req.tone == "professional"  # default
        assert req.email_length == "medium"  # default
        assert req.include_call_to_action is True  # default
    
    def test_email_requirements_tone_validation(self):
        """Test tone validation"""
        # Valid tones
        for tone in ["professional", "casual", "friendly", "formal"]:
            req = EmailRequirements(
                product_service_description="Test product",
                tone=tone
            )
            assert req.tone == tone.lower()
        
        # Invalid tone
        with pytest.raises(ValidationError):
            EmailRequirements(
                product_service_description="Test",
                tone="invalid_tone"
            )
    
    def test_email_requirements_length_validation(self):
        """Test email length validation"""
        # Valid lengths
        for length in ["short", "medium", "long"]:
            req = EmailRequirements(
                product_service_description="Test product",
                email_length=length
            )
            assert req.email_length == length.lower()
        
        # Invalid length
        with pytest.raises(ValidationError):
            EmailRequirements(
                product_service_description="Test",
                email_length="extra_long"
            )
    
    def test_email_requirements_description_too_short(self):
        """Test that short description is rejected"""
        with pytest.raises(ValidationError):
            EmailRequirements(product_service_description="Short")


class TestProspectCriteria:
    """Tests for ProspectCriteria model"""
    
    def test_valid_prospect_criteria(self):
        """Test creating valid ProspectCriteria"""
        criteria = ProspectCriteria(
            company_name="Acme Corp",
            industry="Technology",
            min_size=50,
            max_size=500,
            min_founded_year=2010,
            max_founded_year=2020,
            tech_used=["Python", "AWS"]
        )
        assert criteria.company_name == "Acme Corp"
        assert criteria.min_size == 50
        assert criteria.max_size == 500
    
    def test_prospect_criteria_all_optional(self):
        """Test ProspectCriteria with all fields optional"""
        criteria = ProspectCriteria()
        assert criteria.company_name is None
        assert criteria.industry is None
    
    def test_prospect_criteria_domain_validation(self):
        """Test domain validation"""
        # Valid domain
        criteria = ProspectCriteria(domain="example.com")
        assert criteria.domain == "example.com"
        
        # Invalid domain
        with pytest.raises(ValidationError):
            ProspectCriteria(domain="invalid domain with spaces")
    
    def test_prospect_criteria_size_range_validation(self):
        """Test that max_size must be >= min_size"""
        # Valid range
        criteria = ProspectCriteria(min_size=10, max_size=100)
        assert criteria.min_size == 10
        assert criteria.max_size == 100
        
        # Invalid range
        with pytest.raises(ValidationError):
            ProspectCriteria(min_size=100, max_size=10)
    
    def test_prospect_criteria_year_range_validation(self):
        """Test that max_founded_year must be >= min_founded_year"""
        # Valid range
        criteria = ProspectCriteria(min_founded_year=2000, max_founded_year=2020)
        assert criteria.min_founded_year == 2000
        
        # Invalid range
        with pytest.raises(ValidationError):
            ProspectCriteria(min_founded_year=2020, max_founded_year=2000)


class TestProspectInfo:
    """Tests for ProspectInfo model"""
    
    def test_valid_prospect_info(self):
        """Test creating valid ProspectInfo"""
        prospect = ProspectInfo(
            id="123",
            company="Test Company",
            first_name="John",
            last_name="Doe",
            position="CEO",
            email="john@test.com",
            domain="test.com",
            industry="Technology",
            size=100,
            regions=["US"],
            stack=["Python"],
            keywords=["AI"]
        )
        assert prospect.company == "Test Company"
        assert prospect.domain == "test.com"
    
    def test_prospect_info_minimal(self):
        """Test ProspectInfo with only required fields"""
        prospect = ProspectInfo(
            company="Minimal Company",
            domain="minimal.com"
        )
        assert prospect.company == "Minimal Company"
        assert prospect.first_name is None
    
    def test_prospect_info_domain_validation(self):
        """Test domain validation"""
        # Valid domain
        prospect = ProspectInfo(company="Test", domain="example.com")
        assert prospect.domain == "example.com"
        
        # Invalid domain
        with pytest.raises(ValidationError):
            ProspectInfo(company="Test", domain="invalid domain")
    
    def test_prospect_info_email_validation(self):
        """Test email validation"""
        # Valid email
        prospect = ProspectInfo(
            company="Test",
            domain="test.com",
            email="test@example.com"
        )
        assert prospect.email == "test@example.com"
        
        # Invalid email
        with pytest.raises(ValidationError):
            ProspectInfo(
                company="Test",
                domain="test.com",
                email="invalid-email"
            )


class TestSellerProfile:
    """Tests for SellerProfile model"""
    
    def test_valid_seller_profile(self):
        """Test creating valid SellerProfile"""
        seller = SellerProfile(
            company_name="Seller Corp",
            industry="SaaS",
            company_size=50,
            founded_year=2020,
            value_proposition_keywords=["AI", "automation"],
            target_regions=["US", "EU"]
        )
        assert seller.company_name == "Seller Corp"
        assert seller.industry == "SaaS"
    
    def test_seller_profile_minimal(self):
        """Test SellerProfile with only required fields"""
        seller = SellerProfile(
            company_name="Minimal Seller",
            industry="Technology"
        )
        assert seller.company_name == "Minimal Seller"
        assert seller.company_size is None


class TestEmailGenerationRequest:
    """Tests for EmailGenerationRequest model"""
    
    def test_valid_email_generation_request(self):
        """Test creating valid EmailGenerationRequest"""
        request = EmailGenerationRequest(
            company=CompanyInfo(name="Test Company"),
            contact=ContactInfo(first_name="John"),
            requirements=EmailRequirements(
                product_service_description="Great product for enterprises"
            ),
            sequence_step=1
        )
        assert request.company.name == "Test Company"
        assert request.sequence_step == 1
    
    def test_email_generation_request_sequence_step_validation(self):
        """Test sequence step validation"""
        # Valid sequence step
        request = EmailGenerationRequest(
            company=CompanyInfo(name="Test"),
            contact=ContactInfo(),
            requirements=EmailRequirements(
                product_service_description="Test product"
            ),
            sequence_step=5
        )
        assert request.sequence_step == 5
        
        # Invalid sequence step (too high)
        with pytest.raises(ValidationError):
            EmailGenerationRequest(
                company=CompanyInfo(name="Test"),
                contact=ContactInfo(),
                requirements=EmailRequirements(
                    product_service_description="Test"
                ),
                sequence_step=11
            )


class TestEmailGenerationResponse:
    """Tests for EmailGenerationResponse model"""
    
    def test_email_generation_response_success(self):
        """Test successful EmailGenerationResponse"""
        response = EmailGenerationResponse(
            success=True,
            subject="Test Subject",
            body="Test Body",
            personalization_score=0.85,
            email_id=123
        )
        assert response.success is True
        assert response.subject == "Test Subject"
        assert response.personalization_score == 0.85
    
    def test_email_generation_response_error(self):
        """Test error EmailGenerationResponse"""
        response = EmailGenerationResponse(
            success=False,
            error="Something went wrong"
        )
        assert response.success is False
        assert response.error == "Something went wrong"
        assert response.subject is None
