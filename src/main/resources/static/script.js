let stripe;
let elements; // For one-time payments
let saveCardElements; // For saving cards

let clientSecret; // For one-time payments
let setupIntentClientSecret; // For saving cards

// Persist customer ID in local storage for demonstration purposes
let currentCustomerId = localStorage.getItem('currentCustomerId') || null;

// --- DOM Elements ---
const statusDisplay = document.getElementById('payment-status'); // For one-time payment
const cardErrorsDisplay = document.getElementById('card-errors'); // For one-time payment

const customerStatusDisplay = document.getElementById('customer-status');
const currentCustomerIdDisplay = document.getElementById('current-customer-id');

const saveCardStatusDisplay = document.getElementById('save-card-status');
const saveCardErrorsDisplay = document.getElementById('save-card-errors');

const savedCardsList = document.getElementById('saved-cards-list');
const chargeStatusDisplay = document.getElementById('charge-status');

// --- Buttons ---
const submitButton = document.getElementById('submit-button'); // One-time payment
const createCustomerButton = document.getElementById('create-customer-button');
const saveCardButton = document.getElementById('save-card-button');
const listCardsButton = document.getElementById('list-cards-button');
const chargeCustomerButton = document.getElementById('charge-customer-button');


// --- Initial setup on page load ---
// Fetches publishable key from backend and initializes Stripe elements
fetch('/api/payments/config')
    .then(response => response.json())
    .then(config => {
        stripe = Stripe(config.publishableKey);
        initializeStripeElements();       // For one-time payment
        initializeSaveCardElements();     // For saving cards
        updateCurrentCustomerIdDisplay(); // Update UI with stored customer ID
        if (currentCustomerId) {
            listCustomerCards();          // Load saved cards if customer ID exists
        }
    })
    .catch(error => {
        console.error('Error fetching Stripe config:', error);
        statusDisplay.textContent = 'Failed to load payment configuration.';
    });


// --- General Stripe Element Initialization for one-time payments ---
function initializeStripeElements() {
    elements = stripe.elements();
    const cardElement = elements.create('card', {
        style: {
            base: {
                iconColor: '#666EE8', color: '#313259', fontWeight: '300', fontFamily: '"Helvetica Neue", Helvetica, sans-serif', fontSize: '18px',
                '::placeholder': { color: '#CFD7E0' },
            },
        },
    });
    cardElement.mount('#card-element');
    cardElement.on('change', function(event) {
        cardErrorsDisplay.textContent = event.error ? event.error.message : '';
    });
    submitButton.addEventListener('click', createPaymentIntentAndConfirm);
}

// --- Specific Stripe Element Initialization for Saving Cards ---
function initializeSaveCardElements() {
    saveCardElements = stripe.elements();
    const saveCardElement = saveCardElements.create('card', {
        style: {
            base: {
                iconColor: '#666EE8', color: '#313259', fontWeight: '300', fontFamily: '"Helvetica Neue", Helvetica, sans-serif', fontSize: '18px',
                '::placeholder': { color: '#CFD7E0' },
            },
        },
    });
    saveCardElement.mount('#save-card-element');
    saveCardElement.on('change', function(event) {
        saveCardErrorsDisplay.textContent = event.error ? event.error.message : '';
    });
}

// --- Helper to update current customer ID display in UI ---
function updateCurrentCustomerIdDisplay() {
    currentCustomerIdDisplay.textContent = currentCustomerId || 'None';
    if (currentCustomerId) {
        currentCustomerIdDisplay.classList.add('success-message');
    } else {
        currentCustomerIdDisplay.classList.remove('success-message');
    }
}


// --- 1. Create Customer Functionality ---
createCustomerButton.addEventListener('click', async () => {
    customerStatusDisplay.textContent = 'Creating customer...';
    customerStatusDisplay.className = '';
    const email = document.getElementById('customer-email').value;
    const name = document.getElementById('customer-name').value;

    if (!email || !name) {
        customerStatusDisplay.textContent = 'Email and Name are required.';
        customerStatusDisplay.className = 'error-message';
        return;
    }

    try {
        const response = await fetch('/api/payments/customers', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, name })
        });
        const data = await response.json();

        if (response.ok) {
            currentCustomerId = data.customerId;
            localStorage.setItem('currentCustomerId', currentCustomerId); // Persist customer ID in browser
            updateCurrentCustomerIdDisplay();
            customerStatusDisplay.textContent = `Customer created: ${data.customerId}`;
            customerStatusDisplay.className = 'success-message';
            listCustomerCards(); // Refresh cards list for new customer
        } else {
            customerStatusDisplay.textContent = `Error: ${data.name || 'Failed to create customer.'}`;
            customerStatusDisplay.className = 'error-message';
        }
    } catch (error) {
        console.error('Error creating customer:', error);
        customerStatusDisplay.textContent = 'An error occurred creating customer.';
        customerStatusDisplay.className = 'error-message';
    }
});


// --- 2. Save Card to Customer Functionality ---
saveCardButton.addEventListener('click', async () => {
    if (!currentCustomerId) {
        saveCardStatusDisplay.textContent = 'Please create a customer first.';
        saveCardStatusDisplay.className = 'error-message';
        return;
    }

    saveCardButton.disabled = true;
    saveCardStatusDisplay.textContent = 'Setting up card...';
    saveCardStatusDisplay.className = '';
    saveCardErrorsDisplay.textContent = ''; // Clear previous errors

    try {
        // Step 1: Create a SetupIntent on your backend
        const response = await fetch(`/api/payments/customers/${currentCustomerId}/setup-intent`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
        });
        const data = await response.json();

        if (response.ok) {
            setupIntentClientSecret = data.clientSecret;
            saveCardStatusDisplay.textContent = 'SetupIntent created. Confirming card details...';

            // Step 2: Confirm the SetupIntent on the client-side using Stripe.js
            const { setupIntent, error } = await stripe.confirmCardSetup(setupIntentClientSecret, {
                payment_method: {
                    card: saveCardElements.getElement('card'),
                    // You can add billing_details here if you collect them in your form
                    // billing_details: { name: 'Jenny Rosen', email: 'jenny.rosen@example.com' },
                }
            });

            if (error) {
                if (error.type === "card_error" || error.type === "validation_error") {
                    saveCardErrorsDisplay.textContent = error.message;
                } else {
                    saveCardErrorsDisplay.textContent = "An unexpected error occurred.";
                }
                saveCardStatusDisplay.textContent = 'Failed to save card.';
                saveCardStatusDisplay.className = 'error-message';
            } else if (setupIntent.status === 'succeeded') {
                saveCardStatusDisplay.textContent = `Card saved successfully! Payment Method ID: ${setupIntent.paymentMethod}`;
                saveCardStatusDisplay.className = 'success-message';
                listCustomerCards(); // Refresh the list of saved cards after successful save
            } else {
                saveCardStatusDisplay.textContent = `Setup status: ${setupIntent.status}`;
                saveCardStatusDisplay.className = 'error-message';
            }
        } else {
            saveCardErrorsDisplay.textContent = data.clientSecret || 'Failed to create Setup Intent.';
            saveCardStatusDisplay.textContent = 'Card saving failed during intent creation.';
            saveCardStatusDisplay.className = 'error-message';
        }
    } catch (error) {
        console.error('Error during card saving process:', error);
        saveCardStatusDisplay.textContent = 'An error occurred. Please try again.';
        saveCardStatusDisplay.className = 'error-message';
    } finally {
        saveCardButton.disabled = false;
    }
});


// --- 3. List Saved Cards Functionality ---
listCardsButton.addEventListener('click', listCustomerCards); // Event listener for refresh button

async function listCustomerCards() {
    if (!currentCustomerId) {
        savedCardsList.innerHTML = '<li>No customer selected.</li>';
        return;
    }

    savedCardsList.innerHTML = '<li>Loading cards...</li>';
    try {
        const response = await fetch(`/api/payments/customers/${currentCustomerId}/payment-methods`);
        const cards = await response.json();

        savedCardsList.innerHTML = ''; // Clear previous list items
        if (cards && cards.length > 0) {
            cards.forEach(card => {
                const li = document.createElement('li');
                li.innerHTML = `
                    <span>${card.brand} ending ${card.last4} (Exp: ${card.expMonth}/${card.expYear})</span>
                    <div>
                        <button class="small-button charge-specific-card" data-payment-method-id="${card.id}">Charge This Card</button>
                        <button class="small-button delete-card-button" data-payment-method-id="${card.id}">Delete</button>
                    </div>
                `;
                savedCardsList.appendChild(li);
            });

            // Add event listeners to newly created buttons for charging specific cards
            document.querySelectorAll('.charge-specific-card').forEach(button => {
                button.addEventListener('click', (event) => {
                    const paymentMethodId = event.target.dataset.paymentMethodId;
                    console.log("Charging with specific card ID:", paymentMethodId);
                    // Pass the specific paymentMethodId to the charge function
                    chargeCustomerWithSavedCard(paymentMethodId);
                });
            });

            // Add event listeners to newly created buttons for deleting cards
            document.querySelectorAll('.delete-card-button').forEach(button => {
                button.addEventListener('click', async (event) => {
                    const paymentMethodId = event.target.dataset.paymentMethodId;
                    // Confirm with user before deleting
                    if (confirm(`Are you sure you want to delete card ending ${event.target.closest('li').querySelector('span').textContent.match(/(\d{4})/)[1]}?`)) {
                        await deletePaymentMethod(paymentMethodId);
                    }
                });
            });

        } else {
            savedCardsList.innerHTML = '<li>No saved cards. Save one above!</li>';
        }
    } catch (error) {
        console.error('Error listing cards:', error);
        savedCardsList.innerHTML = '<li>Error loading cards.</li>';
    }
}

// Function to handle deleting a saved payment method
async function deletePaymentMethod(paymentMethodId) {
    try {
        const response = await fetch(`/api/payments/payment-methods/${paymentMethodId}`, {
            method: 'DELETE',
        });
        const data = await response.json();
        if (response.ok) {
            console.log(data.message);
            listCustomerCards(); // Refresh list after successful deletion
        } else {
            console.error('Error deleting card:', data.message);
        }
    } catch (error) {
        console.error('Network error deleting card:', error);
    }
}


// --- 4. Charge with Saved Card Functionality ---
// When the main "Charge Customer" button is clicked, we pass null to indicate
// that the backend should determine which saved card to use (e.g., default or first available).
chargeCustomerButton.addEventListener('click', () => chargeCustomerWithSavedCard(null));

async function chargeCustomerWithSavedCard(paymentMethodId = null) {
    if (!currentCustomerId) {
        chargeStatusDisplay.textContent = 'Please create a customer and save a card first.';
        chargeStatusDisplay.className = 'error-message';
        return;
    }

    chargeCustomerButton.disabled = true;
    chargeStatusDisplay.textContent = 'Charging customer...';
    chargeStatusDisplay.className = '';

    const amount = document.getElementById('charge-amount').value;
    const description = document.getElementById('charge-description').value;

    if (!amount || amount <= 0) {
        chargeStatusDisplay.textContent = 'Please enter a valid amount to charge.';
        chargeStatusDisplay.className = 'error-message';
        chargeCustomerButton.disabled = false;
        return;
    }

    try {
        const response = await fetch('/api/payments/charge-customer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                customerId: currentCustomerId,
                paymentMethodId: paymentMethodId, // Will be null if main button, or ID if specific card button
                amount: parseInt(amount, 10),
                currency: 'usd',
                description: description
            })
        });
        const data = await response.json();

        if (response.ok) {
            if (data.clientSecret) { // Payment requires action (e.g., 3D Secure)
                chargeStatusDisplay.textContent = 'Payment requires action, redirecting for authentication...';
                // Handle 3D Secure or other required actions
                const { error: confirmError } = await stripe.confirmCardPayment(data.clientSecret);
                if (confirmError) {
                    chargeStatusDisplay.textContent = `Authentication failed: ${confirmError.message}`;
                    chargeStatusDisplay.className = 'error-message';
                } else {
                    // Payment succeeded after authentication
                    chargeStatusDisplay.textContent = 'Payment authenticated and succeeded (check webhook/dashboard).';
                    chargeStatusDisplay.className = 'success-message';
                }
            } else { // Payment succeeded directly without further action
                chargeStatusDisplay.textContent = `Charge successful! PaymentIntent ID: ${data.paymentIntentId}`;
                chargeStatusDisplay.className = 'success-message';
            }
        } else {
            chargeStatusDisplay.textContent = `Error: ${data.message || 'Failed to charge customer.'}`;
            chargeStatusDisplay.className = 'error-message';
        }
    } catch (error) {
        console.error('Error charging customer:', error);
        chargeStatusDisplay.textContent = 'An error occurred while charging customer.';
        chargeStatusDisplay.className = 'error-message';
    } finally {
        chargeCustomerButton.disabled = false;
    }
}


// --- Original One-Time Payment Logic (kept for comparison) ---
async function createPaymentIntentAndConfirm() {
    submitButton.disabled = true;
    statusDisplay.textContent = 'Processing payment...';
    statusDisplay.className = '';
    cardErrorsDisplay.textContent = ''; // Clear previous errors

    const amount = document.getElementById('amount').value;
    const description = document.getElementById('description').value;

    if (!amount || amount <= 0) {
        cardErrorsDisplay.textContent = 'Please enter a valid amount.';
        submitButton.disabled = false;
        return;
    }

    try {
        const response = await fetch('/api/payments/create-payment-intent', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                amount: parseInt(amount, 10),
                currency: 'usd', // Hardcoded for this demo
                description: description
            }),
        });

        const data = await response.json();

        if (response.ok) {
            clientSecret = data.clientSecret;
            statusDisplay.textContent = 'PaymentIntent created. Confirming card...';

            const { paymentIntent, error } = await stripe.confirmCardPayment(clientSecret, {
                payment_method: {
                    card: elements.getElement('card'),
                }
            });

            if (error) {
                if (error.type === "card_error" || error.type === "validation_error") {
                    cardErrorsDisplay.textContent = error.message;
                } else {
                    cardErrorsDisplay.textContent = "An unexpected error occurred.";
                }
                statusDisplay.textContent = 'Payment failed.';
                statusDisplay.className = 'error-message';
            } else if (paymentIntent.status === 'succeeded') {
                statusDisplay.textContent = 'Payment succeeded! Transaction ID: ' + paymentIntent.id;
                statusDisplay.className = 'success-message';

                // Optional: Notify your backend of the successful confirmation for final record update
                await fetch('/api/payments/confirm-payment', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ paymentIntentId: paymentIntent.id })
                });

            } else {
                statusDisplay.textContent = `Payment status: ${paymentIntent.status}`;
                statusDisplay.className = 'error-message';
            }
        } else {
            cardErrorsDisplay.textContent = data.clientSecret || 'Failed to create Payment Intent.';
            statusDisplay.textContent = 'Payment failed during intent creation.';
            statusDisplay.className = 'error-message';
        }
    } catch (error) {
        console.error('Error during payment process:', error);
        statusDisplay.textContent = 'An error occurred. Please try again.';
        statusDisplay.className = 'error-message';
    } finally {
        submitButton.disabled = false;
    }
}