import { useState, useEffect } from "react";
import type { DownloadStatus } from "../types/download";

interface SSEParameters {
    requestId: string,
    url?: string,
    downloadStatus?: DownloadStatus
}

interface ProgressData {
    status: string | null,
    code: string | null,
    progress: number,
    message: string | null
}

export const useDownloadProgress = ({requestId, url, downloadStatus}: SSEParameters): ProgressData => {
    const [status, setStatus] = useState<DownloadStatus>(null);
    const [code, setCode] = useState<string | null>(null);
    const [progress, setProgress] = useState<number>(0);
    const [message, setMessage] = useState<string | null>(null);

    const reset = () => {
        setStatus(null);
        setCode(null);
        setProgress(0);
        setMessage(null);
    }

    useEffect(() => {
        if(!requestId || !url || downloadStatus === 'completed') return;

        const eventSource = new EventSource(url);

        eventSource.onopen = () => {
            console.log("SSE Connection opened.");
        }

        eventSource.addEventListener("status", (event: MessageEvent) => {
            const parsedData = JSON.parse(event.data);
            console.log("Status Update:", parsedData);
            if(parsedData.status === "completed" || parsedData.status === "cancelled") {
                reset();
                eventSource.close();
            }
            setStatus(parsedData.status);
        });

        eventSource.addEventListener("progress", (event: MessageEvent) => {
            const parsedData = JSON.parse(event.data);
            console.log("Progress Update:", parsedData);
            setProgress(parsedData.progress);
            setMessage(parsedData.message);
        });

        eventSource.addEventListener("error", (event: MessageEvent) => {
            const parsedData = JSON.parse(event.data);
            console.error("Error Update:", parsedData);
            setStatus("failed");
            setCode(parsedData.code);
            setProgress(0);
            setMessage(parsedData.message);
        });

        eventSource.onerror = (error) => {
            console.error("SSE Error:", error);
            eventSource.close();
        }

        return () => {
            eventSource.close();
        };
    }, [downloadStatus]);

    return { status, code, progress, message };
}

export default useDownloadProgress;