import Venue from "../../components/Venue";
import styles from "./index.module.css";
import { Navigate, useLoaderData, useLocation } from "react-router-dom";

export const concertLoader = async (params) => {
    const { artist, eventID } = params;
    let response = await fetch(`/concerts/${eventID}/seats`);
    let sections = await response.json();
    return { artist, sections, eventID }
}

const Concert = () => {
    const { artist, sections, eventID } = useLoaderData();
    const { state } = useLocation();
    if(!state) return <Navigate to="/events" /> 
    const { description, date, title } = state;

    return (
        <>
        <div className={styles.concertHeader}>
            <h1>{artist}: {title}</h1>
            <div className={styles.concertHeaderDetails}>
                <strong> {description}</strong><br />
                <strong>
                {new Date(date).toLocaleDateString('en-US', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                })}
                </strong>
            </div>
        </div>
        <Venue sections={sections} eventID={eventID} />
        </>
    )
}

export default Concert;