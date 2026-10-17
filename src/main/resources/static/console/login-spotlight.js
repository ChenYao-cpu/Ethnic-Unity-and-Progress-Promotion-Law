// Decorative pointer spotlight; the original heading remains the accessible text.
(() => {
  const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)');
  document.addEventListener('pointerover', event => {
    const heading = event.target.closest('.login-intro h1');
    if (!heading || !finePointer.matches || event.pointerType === 'touch') return;
    if (!heading.querySelector('.login-spotlight-copy')) {
      const copy = document.createElement('span');
      copy.className = 'login-spotlight-copy';
      copy.setAttribute('aria-hidden', 'true');
      for (const child of [...heading.childNodes]) copy.append(child.cloneNode(true));
      heading.append(copy);
    }
  });
  document.addEventListener('pointermove', event => {
    const heading = event.target.closest('.login-intro h1');
    if (!heading || !finePointer.matches) return;
    // Text ranges exclude the empty space in the centered, full-width heading.
    const overText = [...heading.childNodes].some(node => {
      if (node.nodeType !== Node.TEXT_NODE || !node.textContent.trim()) return false;
      const range = document.createRange();
      range.selectNodeContents(node);
      return [...range.getClientRects()].some(rect =>
        event.clientX >= rect.left && event.clientX <= rect.right &&
        event.clientY >= rect.top && event.clientY <= rect.bottom);
    });
    heading.classList.toggle('spotlight-active', overText);
    if (!overText) return;
    const bounds = heading.getBoundingClientRect();
    heading.style.setProperty('--spot-x', `${event.clientX - bounds.left}px`);
    heading.style.setProperty('--spot-y', `${event.clientY - bounds.top}px`);
  });
  document.addEventListener('pointerout', event => {
    const heading = event.target.closest('.login-intro h1');
    if (heading && !heading.contains(event.relatedTarget)) heading.classList.remove('spotlight-active');
  });
})();
