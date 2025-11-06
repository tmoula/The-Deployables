import { useState, useEffect } from "react";
import { Plus, Search } from "lucide-react";
import { api } from "../services/api";

export default function Leads() {
  const [showForm, setShowForm] = useState(false);
  const [leads, setLeads] = useState([]);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    company: "",
    domain: "",
    role: "",
    name: "",
    email: "",
    size: "",
    region: "",
    stack: ""
  });

  // Load prospects when component mounts
  useEffect(() => {
    loadLeads();
  }, []);

  const loadLeads = async () => {
    try {
      setLoading(true);
      const data = await api.listProspects();
      setLeads(data);
    } catch (error) {
      console.error("Error loading leads:", error);
      alert("Failed to load leads. Make sure backend is running on http://localhost:8081");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Temporary local add until backend POST is implemented
    const newLead = {
      id: Date.now().toString(),
      ...formData,
      stack: formData.stack.split(",").map(s => s.trim()).filter(Boolean)
    };

    setLeads([...leads, newLead]);

    setFormData({
      company: "",
      domain: "",
      role: "",
      name: "",
      email: "",
      size: "",
      region: "",
      stack: ""
    });

    setShowForm(false);
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  return (
    <div className="p-6 bg-gray-50 min-h-screen">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-800">Leads</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition"
        >
          <Plus size={20} />
          Add Lead
        </button>
      </div>

      {loading && (
        <div className="bg-blue-50 border border-blue-200 text-blue-700 px-4 py-3 rounded mb-6">
          Loading leads from backend...
        </div>
      )}

      {showForm && (
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">Add New Lead</h2>
          <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <input type="text" name="company" value={formData.company} onChange={handleChange} placeholder="Company Name *" required className="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <input type="text" name="domain" value={formData.domain} onChange={handleChange} placeholder="Domain *" required className="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <input type="text" name="role" value={formData.role} onChange={handleChange} placeholder="Role" className="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <input type="text" name="name" value={formData.name} onChange={handleChange} placeholder="Contact Name" className="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <input type="email" name="email" value={formData.email} onChange={handleChange} placeholder="Email" className="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <input type="number" name="size" value={formData.size} onChange={handleChange} placeholder="Company Size" min="1" max="100000" className="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <input type="text" name="region" value={formData.region} onChange={handleChange} placeholder="Region" className="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <input type="text" name="stack" value={formData.stack} onChange={handleChange} placeholder="Tech Stack (comma-separated)" className="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <div className="md:col-span-2 flex gap-3">
              <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg transition">Save Lead</button>
              <button type="button" onClick={() => setShowForm(false)} className="bg-gray-300 hover:bg-gray-400 text-gray-800 px-6 py-2 rounded-lg transition">Cancel</button>
            </div>
          </form>
        </div>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center gap-2 border border-gray-300 rounded px-3 py-2 max-w-md">
            <Search size={20} className="text-gray-400" />
            <input type="text" placeholder="Search leads..." className="flex-1 outline-none" />
          </div>
        </div>

        {leads.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            {loading ? "Loading..." : 'No leads yet. Click "Add Lead" to get started.'}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Company</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Domain</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Contact</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Size</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Region</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {leads.map((lead) => (
                  <tr key={lead.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm font-medium text-gray-900">{lead.company}</td>
                    <td className="px-6 py-4 text-sm text-gray-500">{lead.domain}</td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {lead.name && lead.email ? `${lead.name} (${lead.email})` : lead.email || lead.name || "-"}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">{lead.size || "-"}</td>
                    <td className="px-6 py-4 text-sm text-gray-500">{lead.region || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
