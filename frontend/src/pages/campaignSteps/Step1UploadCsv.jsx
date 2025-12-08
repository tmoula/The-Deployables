import { Upload, CheckCircle } from "lucide-react";

export default function Step1UploadCsv({
  campaignName,
  setCampaignName,
  description,
  setDescription,
  campaignNameError,
  setCampaignNameError,
  uploadedFile,
  setUploadedFile,
  isDragging,
  handleDragOver,
  handleDragLeave,
  handleDrop,
  handleFileInputSelect,
  uploading,
  isEditing = false
}) {
  return (
    <div>
      <div className="mb-4 grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Campaign Name <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={campaignName}
            onChange={(e) => {
              setCampaignName(e.target.value);
              if (campaignNameError) setCampaignNameError("");
            }}
            placeholder="e.g., Q4 Outreach Campaign"
            required
            className={`w-full border rounded px-3 py-2 focus:outline-none focus:ring-2 ${
              campaignNameError
                ? "border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-blue-500"
            }`}
          />
          {campaignNameError && (
            <p className="text-red-500 text-xs mt-1">
              {campaignNameError}
            </p>
          )}
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Description (Optional)
          </label>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Brief description of the campaign"
            className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors ${
          isDragging
            ? "border-blue-500 bg-blue-50"
            : "border-gray-300 hover:border-gray-400 bg-gray-50"
        }`}
      >
        {uploadedFile ? (
          <div className="flex flex-col items-center w-full">
            <CheckCircle
              size={48}
              className="text-green-600 mb-4"
            />
            <div className="text-center mb-4 w-full">
              <p className="text-lg font-medium text-gray-700 mb-1">
                {uploadedFile.name}
              </p>
              {uploadedFile.size && (
                <p className="text-sm text-gray-500 mb-2">
                  {(uploadedFile.size / 1024).toFixed(2)} KB
                </p>
              )}
              {uploadedFile.leadsCount !== undefined && (
                <p className="text-sm font-semibold text-blue-600 mb-2">
                  {uploadedFile.leadsCount} leads detected
                </p>
              )}
              {isEditing && (
                <p className="text-sm text-gray-500 mb-2">
                  CSV file already uploaded for this campaign
                </p>
              )}
            </div>
            
            {/* Show detected columns */}
            {uploadedFile.columns && uploadedFile.columns.length > 0 && (
              <div className="w-full mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
                <p className="text-sm font-semibold text-gray-700 mb-2">
                  📋 Detected Columns ({uploadedFile.columns.length}):
                </p>
                <div className="flex flex-wrap gap-2">
                  {uploadedFile.columns.map((column, index) => (
                    <span
                      key={index}
                      className="px-2 py-1 bg-white border border-blue-300 rounded text-xs text-gray-700 font-mono"
                      title={`Use {{${column.toLowerCase().replace(/\s+/g, '_')}}} in your emails`}
                    >
                      {column}
                    </span>
                  ))}
                </div>
                <p className="text-xs text-gray-600 mt-2">
                  💡 Tip: Use these columns as variables in your emails (e.g., {`{{${uploadedFile.columns[0]?.toLowerCase().replace(/\s+/g, '_') || 'column_name'}}}`})
                </p>
              </div>
            )}
            
            <button
              onClick={() => setUploadedFile(null)}
              className="text-sm text-blue-600 hover:text-blue-800 mt-4"
            >
              {isEditing ? "Replace file" : "Change file"}
            </button>
          </div>
        ) : (
          <>
            <Upload
              size={48}
              className={`mx-auto mb-4 ${
                isDragging ? "text-blue-600" : "text-gray-400"
              }`}
            />
            <h3 className="text-lg font-medium text-gray-700 mb-2">
              {isDragging
                ? "Drop your CSV file here"
                : "Drag & drop your CSV file here"}
            </h3>
            <p className="text-sm text-gray-500 mb-4">or</p>
            <label className="inline-block bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg cursor-pointer transition">
              <input
                type="file"
                accept=".csv"
                onChange={handleFileInputSelect}
                className="hidden"
                disabled={uploading}
              />
              Browse Files
            </label>
            <p className="text-xs text-gray-500 mt-4">
              {isEditing 
                ? "You can upload a new CSV file to replace the existing leads, or skip to continue editing other settings."
                : "Upload the CSV file downloaded from \"Your Future Clients\" page"}
            </p>
          </>
        )}
      </div>
    </div>
  );
}

