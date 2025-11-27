import { Button } from '@mantine/core';

interface ProgressBarProps {
    progress: number
    fillColor: string,
    message?: string
}

const ProgressBar = ({progress, fillColor, message}: ProgressBarProps) => {

    const parentStyle = {
        position: "relative" as const,
        width: "100%",
        height: "35px"
    }

    const backgroundStyle = {
        position: "absolute" as const,
        width: "100%",
        height: "100%",
        backgroundColor: "gray",
        borderRadius: "5px"
    };

    const foregroundStyle = {
        position: "absolute" as const,
        width: `${progress}%`,
        height: "100%",
        backgroundColor: fillColor,
        borderRadius: "5px",
        transition: "width 0.5s ease-in-out"
    }

    const textStyle = {
        position: "absolute" as const,
        width: "100%",
        height: "100%",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        color: "white",
        fontWeight: "bold" as const
    }

    return (
        <div style={parentStyle}>
            <div style={backgroundStyle}>

            </div>
            <div style={foregroundStyle}>

            </div>
            <div style={textStyle}>
                {
                    message ? message : `${progress}%`
                }
            </div>
        </div>
    );
}

export default ProgressBar;