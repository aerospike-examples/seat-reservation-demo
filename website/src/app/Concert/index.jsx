import { useState } from "react";
import Header from "../../components/Header";
import Venue from "../../components/Venue";
import styles from "./index.module.css";
import { Navigate, useLoaderData, useLocation } from "react-router-dom";

export const concertLoader = async (params) => {
    const apiUrl = import.meta.env.VITE_APP_API_URL;
    const { artist, eventID } = params;
    let response = await fetch(`${apiUrl}/concerts/${eventID}/seats`);
    let data = await response.json();
    return { artist, data }
}

const Concert = () => {
    const { artist, data } = useLoaderData();
    const { state } = useLocation();
    const [sections, setSections] = useState(data);
    const [venueKey, setVenueKey] = useState(0);
    if(!state) return <Navigate to="/events" /> 
    const { description, date, title, eventID } = state;

    return (
        <>
        <Header eventID={eventID} setSections={setSections} setVenueKey={setVenueKey}/>
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
        <Venue sections={sections} eventID={eventID} key={venueKey} />
        </>
    )
}

export default Concert;