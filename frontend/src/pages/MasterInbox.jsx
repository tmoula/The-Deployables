import React, { useState, useEffect } from 'react';
import { mailboxApi } from '../services/mailboxApi';
import Topbar from "../components/Topbar";

export default function MasterInbox() {
    const [emails, setEmails] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedEmail, setSelectedEmail] = useState(null);

    useEffect(() => {
        loadEmails();
    }, []);

    const loadEmails = async () => {
        try {
            setLoading(true);
            const data = await mailboxApi.getAllEmails();
            setEmails(data);
            setError(null);
        } catch (err) {
            console.error('Failed to load emails:', err);
            setError('Failed to load emails. Make sure you have connected a mailbox.');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <>
                <Topbar />
                <div className="flex items-center justify-center h-screen bg-gray-50">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
                </div>
            </>
        );
    }

    return (
        <div className="h-screen flex flex-col bg-gray-50">
            <Topbar />

            <div className="flex-1 flex overflow-hidden max-w-7xl w-full mx-auto p-4 md:p-8">
                <div className="bg-white rounded-lg shadow-lg flex w-full overflow-hidden h-[calc(100vh-8rem)]">

                    {/* Email List */}
                    <div className={`${selectedEmail ? 'hidden md:flex' : 'flex'} w-full md:w-1/3 flex-col border-r border-gray-200`}>
                        <div className="p-4 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                            <h2 className="font-semibold text-gray-700">Inbox</h2>
                            <button
                                onClick={loadEmails}
                                className="text-indigo-600 hover:text-indigo-800 text-sm font-medium"
                            >
                                Refresh
                            </button>
                        </div>

                        <div className="flex-1 overflow-y-auto">
                            {error && (
                                <div className="p-4 bg-red-50 text-red-700 text-sm border-b border-red-100">
                                    {error}
                                </div>
                            )}

                            {emails.length === 0 && !error ? (
                                <div className="p-8 text-center text-gray-500">
                                    <p>No emails found.</p>
                                    <p className="text-sm mt-2">Connect a mailbox in the Mailboxes tab to get started.</p>
                                </div>
                            ) : (
                                emails.map((email) => (
                                    <div
                                        key={email.id}
                                        onClick={() => setSelectedEmail(email)}
                                        className={`p-4 border-b border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors ${selectedEmail?.id === email.id ? 'bg-indigo-50' : ''
                                            } ${!email.isRead ? 'font-semibold' : ''}`}
                                    >
                                        <div className="flex justify-between items-start mb-1">
                                            <span className="text-sm font-medium text-gray-900 truncate w-2/3">
                                                {email.from}
                                            </span>
                                            <span className="text-xs text-gray-500 whitespace-nowrap">
                                                {new Date(email.receivedAt).toLocaleDateString()}
                                            </span>
                                        </div>
                                        <div className="text-sm text-gray-800 mb-1 truncate">
                                            {email.subject}
                                        </div>
                                        <div className="text-xs text-gray-500 truncate">
                                            {email.snippet}
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>

                    {/* Email Detail */}
                    <div className={`${selectedEmail ? 'flex' : 'hidden md:flex'} flex-1 flex-col bg-white`}>
                        {selectedEmail ? (
                            <div className="flex flex-col h-full">
                                <div className="p-6 border-b border-gray-100">
                                    <button
                                        onClick={() => setSelectedEmail(null)}
                                        className="md:hidden mb-4 text-indigo-600 font-medium flex items-center"
                                    >
                                        ← Back to list
                                    </button>

                                    <h2 className="text-xl font-bold text-gray-900 mb-4">
                                        {selectedEmail.subject}
                                    </h2>

                                    <div className="flex justify-between items-start">
                                        <div>
                                            <div className="font-medium text-gray-900">
                                                {selectedEmail.from}
                                            </div>
                                            <div className="text-sm text-gray-500">
                                                To: {selectedEmail.mailboxEmail}
                                            </div>
                                        </div>
                                        <div className="text-sm text-gray-500">
                                            {new Date(selectedEmail.receivedAt).toLocaleString()}
                                        </div>
                                    </div>
                                </div>

                                <div className="flex-1 p-6 overflow-y-auto bg-gray-50">
                                    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
                                        <p className="whitespace-pre-wrap text-gray-800">{selectedEmail.snippet}</p>
                                        <p className="mt-8 pt-4 border-t border-gray-100 italic text-gray-400 text-sm">
                                            (Full email body fetching not yet implemented in simplified version)
                                        </p>
                                    </div>
                                </div>

                                <div className="p-4 border-t border-gray-200 bg-white flex gap-3">
                                    <button className="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 text-sm font-medium">
                                        Reply
                                    </button>
                                    <button className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 text-sm font-medium">
                                        Forward
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div className="flex-1 flex flex-col items-center justify-center text-gray-400 bg-gray-50">
                                <svg className="w-16 h-16 mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                                </svg>
                                <p>Select an email to view details</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
