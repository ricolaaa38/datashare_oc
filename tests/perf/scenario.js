import http from "k6/http";
import { check, sleep } from "k6";
import { Trend } from "k6/metrics";

const uploadDuration = new Trend("upload_duration", true);
const metadataDuration = new Trend("metadata_duration", true);
const downloadDuration = new Trend("download_duration", true);

export const options = {
  vus: 10,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.01"],
    upload_duration: ["p(95)<1500"],
    metadata_duration: ["p(95)<500"],
    download_duration: ["p(95)<1500"],
  },
};

const backendUrl = __ENV.BACKEND_URL || "http://localhost:8080";

export default function () {
  const health = http.get(`${backendUrl}/actuator/health`);
  check(health, { "health status is 200": (response) => response.status === 200 });

  const upload = http.post(
    `${backendUrl}/anonymous/files`,
    {
      file: http.file(`performance-${__VU}-${__ITER}.txt`, "performance.txt", "text/plain"),
      originalName: "performance.txt",
      mimeType: "text/plain",
      expiresInDays: "1",
    },
    { tags: { operation: "upload" } },
  );
  uploadDuration.add(upload.timings.duration);
  const uploadOk = check(upload, {
    "upload status is 201": (response) => response.status === 201,
    "upload returns a download token": (response) => Boolean(response.json("downloadToken")),
  });

  if (uploadOk) {
    const token = upload.json("downloadToken");
    const metadata = http.get(`${backendUrl}/downloads/${token}/metadata`, {
      tags: { operation: "metadata" },
    });
    metadataDuration.add(metadata.timings.duration);
    check(metadata, { "metadata status is 200": (response) => response.status === 200 });

    const download = http.get(`${backendUrl}/downloads/${token}`, {
      tags: { operation: "download" },
    });
    downloadDuration.add(download.timings.duration);
    check(download, {
      "download status is 200": (response) => response.status === 200,
      "download body is not empty": (response) => response.body.length > 0,
    });
  }

  sleep(1);
}
