import { useState, useEffect } from 'react';
import Topbar from "../components/Topbar";
import { mailboxApi } from '../services/mailboxApi';
import { Mail, Plus, Pause, Play, Trash2, TestTube, AlertCircle, CheckCircle } from 'lucide-react';

export default function Mailboxes() {
    const [mailboxes, setMailboxes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showAddModal, setShowAddModal] = useState(false);
    const [formData, setFormData] = useState({
        email: '',
        displayName: '',
        appPassword: ''
    });
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    useEffect(() => {
        loadMailboxes();
    }, []);

    const loadMailboxes = async () => {
        try {
            setLoading(true);
            const data = await mailboxApi.getMailboxes();
            setMailboxes(data);
        } catch (err) {
            setError('Failed to load mailboxes: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleAddMailbox = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        try {
            await mailboxApi.addGmail(formData);
            setSuccess('Gmail account connected successfully!');
            setShowAddModal(false);
            setFormData({ email: '', displayName: '', appPassword: '' });
            loadMailboxes();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleToggleStatus = async (mailbox) => {
        const newStatus = mailbox.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
        try {
            await mailboxApi.updateStatus(mailbox.id, newStatus);
            loadMailboxes();
        } catch (err) {
            setError('Failed to update status: ' + err.message);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure you want to disconnect this mailbox?')) {
            return;
        }

        try {
            await mailboxApi.removeMailbox(id);
            setSuccess('Mailbox disconnected successfully');
            loadMailboxes();
        } catch (err) {
            setError('Failed to remove mailbox: ' + err.message);
        }
    };

    const handleTest = async (id) => {
        try {
            const result = await mailboxApi.testConnection(id);
            if (result.connected) {
                setSuccess('Connection test successful!');
            } else {
                setError('Connection test failed');
            }
        } catch (err) {
            setError('Connection test failed: ' + err.message);
        }
    };

    return (
        <>
            <Topbar />
            <div className="p-8">
                <div className="max-w-7xl mx-auto">
                    {/* Header */}
                    <div className="flex justify-between items-center mb-6">
                        <div>
                            <h1 className="text-3xl font-bold text-gray-900">Mailboxes</h1>
                            <p className="text-gray-600 mt-1">Connect your Gmail accounts to send campaigns</p>
                        </div>
                        <button
                            onClick={() => setShowAddModal(true)}
                            className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
                        >
                            <Plus size={20} />
                            Connect Gmail
                        </button>
                    </div>

                    {/* Alerts */}
                    {error && (
                        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-red-800">
                            <AlertCircle size={20} />
                            {error}
                        </div>
                    )}
                    {success && (
                        <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center gap-2 text-green-800">
                            <CheckCircle size={20} />
                            {success}
                        </div>
                    )}

                    {/* Mailboxes List */}
                    {loading ? (
                        <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
                            Loading mailboxes...
                        </div>
                    ) : mailboxes.length === 0 ? (
                        <div className="bg-white rounded-lg shadow p-8 text-center">
                            <Mail size={48} className="mx-auto text-gray-400 mb-4" />
                            <h3 className="text-lg font-semibold text-gray-900 mb-2">No mailboxes connected</h3>
                            <p className="text-gray-600 mb-4">Connect your Gmail account to start sending campaigns</p>
                            <button
                                onClick={() => setShowAddModal(true)}
                                className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition"
                            >
                                Connect Gmail
                            </button>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {mailboxes.map((mailbox) => (
                                <div key={mailbox.id} className="bg-white rounded-lg shadow p-6 border border-gray-200">
                                    <div className="flex items-start justify-between mb-4">
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                                                <Mail size={20} className="text-blue-600" />
                                            </div>
                                            <div>
                                                <h3 className="font-semibold text-gray-900">{mailbox.displayName || 'Gmail'}</h3>
                                                <p className="text-sm text-gray-600">{mailbox.email}</p>
                                            </div>
                                        </div>
                                        <span className={`px-2 py-1 text-xs rounded-full ${mailbox.status === 'ACTIVE'
                                                ? 'bg-green-100 text-green-800'
                                                : 'bg-yellow-100 text-yellow-800'
                                            }`}>
                                            {mailbox.status}
                                        </span>
                                    </div>

                                    {/* Actions */}
                                    <div className="flex gap-2">
                                        <button
                                            onClick={() => handleToggleStatus(mailbox)}
                                            className="flex-1 flex items-center justify-center gap-1 px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded text-sm transition"
                                            title={mailbox.status === 'ACTIVE' ? 'Pause' : 'Resume'}
                                        >
                                            {mailbox.status === 'ACTIVE' ? <Pause size={16} /> : <Play size={16} />}
                                            {mailbox.status === 'ACTIVE' ? 'Pause' : 'Resume'}
                                        </button>
                                        <button
                                            onClick={() => handleTest(mailbox.id)}
                                            className="flex-1 flex items-center justify-center gap-1 px-3 py-2 bg-blue-50 hover:bg-blue-100 text-blue-700 rounded text-sm transition"
                                            title="Test Connection"
                                        >
                                            <TestTube size={16} />
                                            Test
                                        </button>
                                        <button
                                            onClick={() => handleDelete(mailbox.id)}
                                            className="px-3 py-2 bg-red-50 hover:bg-red-100 text-red-700 rounded text-sm transition"
                                            title="Disconnect"
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Help Text */}
                    <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
                        <h4 className="font-semibold text-blue-900 mb-2">How to get a Gmail App Password:</h4>
                        <ol className="text-sm text-blue-800 space-y-1 list-decimal list-inside">
                            <li>Go to your Google Account settings</li>
                            <li>Enable 2-Step Verification if not already enabled</li>
                            <li>Go to Security → 2-Step Verification → App passwords</li>
                            <li>Generate a new app password for "Mail"</li>
                            <li>Copy the 16-character password and use it here</li>
                        </ol>
                    </div>
                </div>
            </div>

            {/* Add Mailbox Modal */}
            {showAddModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
                        <h2 className="text-2xl font-bold mb-4">Connect Gmail Account</h2>
                        <form onSubmit={handleAddMailbox}>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Gmail Address *
                                    </label>
                                    <input
                                        type="email"
                                        required
                                        value={formData.email}
                                        onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                        placeholder="your.email@gmail.com"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        Display Name
                                    </label>
                                    <input
                                        type="text"
                                        value={formData.displayName}
                                        onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                        placeholder="My Gmail Account"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">
                                        App Password *
                                    </label>
                                    <input
                                        type="password"
                                        required
                                        value={formData.appPassword}
                                        onChange={(e) => setFormData({ ...formData, appPassword: e.target.value })}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                        placeholder="16-character app password"
                                    />
                                    <p className="text-xs text-gray-500 mt-1">
                                        Generate this from Google Account → Security → App passwords
                                    </p>
                                </div>
                            </div>

                            <div className="flex gap-3 mt-6">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowAddModal(false);
                                        setError('');
                                    }}
                                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
                                >
                                    Connect
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </>
    );
}
