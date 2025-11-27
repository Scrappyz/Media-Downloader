import { useState, useEffect } from "react";

interface SSEParameters {
    url: string | null
}

export const useSSE = ({url}: SSEParameters) => {
    const [data, setData] = useState<string | null>(null);

    useEffect(() => {
        if (!url) return;

        const eventSource = new EventSource(url);

        eventSource.onmessage = (event) => {
            console.log("SSE Message:", event.data);
            setData(event.data);
        }

        eventSource.onerror = (error) => {
            console.error("SSE Error:", error);
            eventSource.close();
        }

        return () => {
            eventSource.close();
        };
    }, []);

    return data;
}