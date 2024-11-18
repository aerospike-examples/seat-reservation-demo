const { parentPort } = require('worker_threads');
const { v4: uuidv4 } = require('uuid');

const apiUrl = "http://localhost:8080";
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
    let response = await fetch(`${apiUrl}/concerts/${eventID}/shopping-carts/${cartID}`, {
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

parentPort.on("message", (data) => {
    const { idx, delay, seats, abandon, eventID } = data;
    simulateUser(delay, seats, abandon, eventID)
    .then(
        () => parentPort.postMessage({status: "ok", idx, eventID}),
        (err) => parentPort.postMessage({status: err.message, idx, eventID})
    );
})