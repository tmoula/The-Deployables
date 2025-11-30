import Topbar from "../components/Topbar";

export default function MasterInbox() {
    return (
        <>
            <Topbar />
            <div className="p-8">
                <div className="max-w-7xl mx-auto">
                    <h1 className="text-3xl font-bold text-gray-900 mb-6">Master Inbox</h1>
                    <div className="bg-white rounded-lg shadow p-6">
                        <p className="text-gray-600">
                            This is the Master Inbox page. Content coming soon.
                        </p>
                    </div>
                </div>
            </div>
        </>
    );
}
