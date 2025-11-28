import { Button } from '@mantine/core';

interface ProgressBarProps {
    progress: number
    message?: string,
    style?: ProgressBarStyle
}

interface ProgressBarStyle {
    width?: string,
    height?: string,
    backgroundColor?: string,
    fillColor?: string,
    borderRadius?: string,
    fontFamily?: string,
    fontStyle?: string,
    fontWeight?: string | number,
    fontSize?: string,
    textColor?: string
}

const ProgressBar = ({progress, message, style}: ProgressBarProps) => {

    const defaultStyle: ProgressBarStyle = {
        width: "100%",
        height: "35px",
        backgroundColor: "gray",
        fillColor: "green",
        borderRadius: "5px",
        fontFamily: "Arial, sans-serif",
        fontStyle: "normal",
        fontWeight: "bold",
        fontSize: "16px",
        textColor: "white"
    }

    style = {...defaultStyle, ...style};

    const parentStyle = {
        position: "relative" as const,
        width: style.width,
        height: style.height
    }

    const backgroundStyle = {
        position: "absolute" as const,
        width: style.width,
        height: style.height,
        backgroundColor: style.backgroundColor,
        borderRadius: style.borderRadius
    };

    const foregroundStyle = {
        position: "absolute" as const,
        width: `${progress}%`,
        height: style.height,
        backgroundColor: style.fillColor,
        borderRadius: style.borderRadius,
        transition: "width 0.5s ease-in-out"
    }

    const textStyle = {
        position: "absolute" as const,
        width: style.width,
        height: style.height,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        color: style.textColor
    }

    return (
        <div style={parentStyle}>
            <div style={backgroundStyle}>

            </div>
            <div style={foregroundStyle}>

            </div>
            <div style={textStyle}>
                {message}
            </div>
        </div>
    );
}

export default ProgressBar;