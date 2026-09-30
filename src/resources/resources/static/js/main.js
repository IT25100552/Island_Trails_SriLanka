document.addEventListener('DOMContentLoaded', () => {
    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 5000);
    });

    // Password strength hint handler
    const passwordInput = document.querySelector('input[type="password"]#password');
    if (passwordInput) {
        passwordInput.addEventListener('input', (e) => {
            const val = e.target.value;
            const hasUpper = /[A-Z]/.test(val);
            const hasNumber = /[0-9]/.test(val);
            const hasSpecial = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(val);
            const hasLen = val.length >= 8;

            const hint = document.getElementById('password-hint');
            if (hint) {
                if (hasUpper && hasNumber && hasSpecial && hasLen) {
                    hint.className = 'form-text text-success';
                    hint.textContent = '✓ Strong password';
                } else {
                    hint.className = 'form-text text-muted';
                    hint.textContent = 'Min 8 chars, 1 uppercase, 1 number, 1 special character';
                }
            }
        });
    }
});
