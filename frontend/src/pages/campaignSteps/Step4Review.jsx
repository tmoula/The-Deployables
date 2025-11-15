export default function Step4Review({
  campaignName,
  description,
  uploadedFile,
  emailSubject,
  emailContent,
  campaignSettings
}) {
  return (
    <div className="space-y-4">
      <div className="bg-gray-50 p-4 rounded-lg">
        <h3 className="font-semibold text-gray-800 mb-2">
          Campaign Details
        </h3>
        <p>
          <span className="font-medium">Name:</span>{" "}
          {campaignName}
        </p>
        {description && (
          <p>
            <span className="font-medium">
              Description:
            </span>{" "}
            {description}
          </p>
        )}
        <p>
          <span className="font-medium">File:</span>{" "}
          {uploadedFile?.name}
        </p>
      </div>
      <div className="bg-gray-50 p-4 rounded-lg">
        <h3 className="font-semibold text-gray-800 mb-2">
          Email
        </h3>
        <p>
          <span className="font-medium">Subject:</span>{" "}
          {emailSubject || "Not set"}
        </p>
        <p>
          <span className="font-medium">Content:</span>
        </p>
        <div className="mt-2 p-3 bg-white border border-gray-200 rounded text-sm">
          {emailContent || "Not set"}
        </div>
      </div>
      <div className="bg-gray-50 p-4 rounded-lg">
        <h3 className="font-semibold text-gray-800 mb-2">
          Settings
        </h3>
        <p>
          <span className="font-medium">Send Delay:</span>{" "}
          {campaignSettings.sendDelay} days
        </p>
        <p>
          <span className="font-medium">
            Follow-up Delay:
          </span>{" "}
          {campaignSettings.followUpDelay} days
        </p>
        <p>
          <span className="font-medium">
            Max Follow-ups:
          </span>{" "}
          {campaignSettings.maxFollowUps}
        </p>
        <p>
          <span className="font-medium">Send Time:</span>{" "}
          {campaignSettings.sendTime}
        </p>
      </div>
    </div>
  );
}

