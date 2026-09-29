const form = document.querySelector('#shorten-form');
const urlInput = document.querySelector('#url');
const shortenButton = document.querySelector('#shorten');
const error = document.querySelector('#error');
const result = document.querySelector('#result');
const shortLink = document.querySelector('#short-link');
const copyButton = document.querySelector('#copy');
const copyStatus = document.querySelector('#copy-status');

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.textContent = '';
    copyStatus.textContent = '';
    result.hidden = true;
    shortenButton.disabled = true;
    shortenButton.textContent = 'Shortening...';

    try {
        const response = await fetch('/shorten', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ url: urlInput.value.trim() })
        });
        const data = await response.json();
        if (!response.ok) {
            error.textContent = data.error || 'Could not shorten this URL.';
            return;
        }
        const link = `${window.location.origin}/s/${data.shortCode}`;
        shortLink.href = link;
        shortLink.textContent = link;
        result.hidden = false;
        shortLink.focus();
    } catch {
        error.textContent = 'Could not connect to the server. Please try again.';
    } finally {
        shortenButton.disabled = false;
        shortenButton.textContent = 'Shorten';
    }
});

copyButton.addEventListener('click', async () => {
    try {
        await navigator.clipboard.writeText(shortLink.href);
        copyStatus.textContent = 'Copied!';
    } catch {
        copyStatus.textContent = 'Could not copy. Select the link and copy it manually.';
    }
});
