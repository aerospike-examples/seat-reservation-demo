import { useState } from "react";
import { useShow } from "./useShow";

//useModal is exported
export const useModal = (danger = false, openCallback = () => {}, closeCallback = () => {}) => {
    const [open, setOpen] = useState(false);
    
    const callback = () => {
        setTimeout(() => {
            setOpen(false);
            closeCallback();
        }, 300);
    };
    
    const [ showRef, show, setShow ] = useShow(danger, callback);

    const openModal = () => {
        setOpen(true);
        openCallback();
        setTimeout(() => setShow(true), 100);
    }
    const closeModal = () => {
        setShow(false);
        callback();
    }

    return { modalProps: {open, show, closeModal, danger}, ref: showRef, openModal, closeModal }
}