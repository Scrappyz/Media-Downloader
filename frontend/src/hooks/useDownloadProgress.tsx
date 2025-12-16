import { useState, useEffect } from "react";

interface SSEParameters {
    requestId: string,
    url: string | null
}

interface ProgressData {
    status: string | null,
    code: string | null,
    progress: number,
    message: string | null
}

export const useDownloadProgress = ({requestId, url}: SSEParameters): ProgressData => {
    const [status, setStatus] = useState<string | null>(null);
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
        if(!requestId || !url) return;

        const eventSource = new EventSource(url);

        eventSource.onopen = () => {
            console.log("SSE Connection opened.");
        }

        eventSource.addEventListener("status", (event: MessageEvent) => {
            const parsedData = JSON.parse(event.data);
            console.log("Status Update:", parsedData);
            if(parsedData.status === "success" || parsedData.status === "cancelled") {
                reset();
                eventSource.close();
            }
            setStatus(parsedData.status);
            // console.log("Current Status:", parsedData.status);
        });

        eventSource.addEventListener("progress", (event: MessageEvent) => {
            const parsedData = JSON.parse(event.data);
            console.log("Progress Update:", parsedData);
            setProgress(parsedData.progress);
            setMessage(parsedData.message);
            // console.log("Current Progress:", parsedData.progress);
        });

        eventSource.addEventListener("error", (event: MessageEvent) => {
            const parsedData = JSON.parse(event.data);
            console.log("Error Update:", parsedData);
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
    }, [requestId]);

    return { status, progress, message };
}

export default useDownloadProgress;