import { v4 as uuidv4 } from 'uuid';

const apiUrl = import.meta.env.VITE_APP_API_URL;
const sleep = (seconds) =>  new Promise(r => setTimeout(r, seconds * 1000))

const addToCart = async (cartID, numSeats, eventID) => {
    let response = await fetch(`${apiUrl}/concerts/${eventID}/shopping-carts`, {
        headers: {
            "Content-Type": "application/json"
        },
        method: 'POST',
        body: JSON.stringify({id: cartID, randomSeatQuantity: numSeats})
    })
    if(response.ok) return;
    if(response.status === 404) throw new Error("Seats unavailable");
}

const purchaseSeats = async (cartID, eventID) => {
    let response = await fetch(`${apiUrl}/concerts/${eventID}/purchases`, {
        headers: {
            "Content-Type": "application/json"
        },
        method: 'POST',
        body: JSON.stringify({id: cartID})
    })
    if(response.ok) return
    throw new Error("Purchase attempt failed");
}

const abandonSeats = async (cartID, eventID) => {
    let response = await fetch(`${apiUrl}/${eventID}/shopping-carts/${cartID}`, {
        method: 'DELETE'
    })
    if(response.ok) return
    throw new Error("Abandon attempt failed");
}

const simulateUser = async (delay, seats, abandon, eventID) => {
    return new Promise(async (resolve, reject) => {
        let cartID = uuidv4();
        addToCart(cartID, seats, eventID)
        .then(async () => {
            await sleep(delay);
            let random = Math.random();
            if(random < (abandon / 100)){
                abandonSeats(cartID, eventID)
                .then(async () => {
                    await sleep(delay);
                    resolve();
                }, (err) => reject(err));
            }
            else {
                purchaseSeats(cartID, eventID)
                .then(async () => {
                    await sleep(delay);
                    resolve();
                }, (err) => reject(err));
            }
        }, (err) => reject(err));
    });
}

self.onmessage = async (e) => {
    const { idx, delay, seats, abandon, eventID } = e.data;
    simulateUser(delay, seats, abandon, eventID)
    .then(
        () => self.postMessage({status: "ok", idx}),
        (err) => self.postMessage({status: err.message, idx})
    );
};