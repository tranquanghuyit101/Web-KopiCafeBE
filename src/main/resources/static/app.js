(function() {
	const e = React.createElement;

	const firstSegment = window.location.pathname.split('/').filter(Boolean)[0];
	const contextPath = firstSegment ? '/' + firstSegment : '';
	const api = (p) => contextPath + p;

	function App() {
		const [username, setUsername] = React.useState("");
		const [password, setPassword] = React.useState("");
		const [error, setError] = React.useState("");
		const [me, setMe] = React.useState(null);

		React.useEffect(() => {
			fetch(api('/api/auth/me'), { credentials: 'include' })
				.then(r => { if (r.ok) { window.location.assign(contextPath + '/menu'); } })
				.catch(() => {});
		}, []);

		function onSubmit(ev) {
			ev.preventDefault();
			setError("");
			fetch(api('/api/auth/login'), {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				credentials: 'include',
				body: JSON.stringify({ username, password })
			}).then(async r => {
				if (r.ok) {
					window.location.assign(contextPath + '/menu');
				} else {
					setError('Sai tài khoản hoặc mật khẩu');
				}
			}).catch(() => setError('Sai tài khoản hoặc mật khẩu'));
		}

		function logout() {
			fetch(api('/api/auth/logout'), { method: 'POST', credentials: 'include' })
				.then(() => { setMe(null); setUsername(""); setPassword(""); setError(""); })
				.catch(() => {});
		}

		if (me) {
			return e('div', { style: { fontFamily: 'sans-serif', padding: 24 } }, [
				e('h2', { key: 'h2' }, 'Đang chuyển hướng...')
			]);
		}

		return e('div', { style: { maxWidth: 320, margin: '80px auto', fontFamily: 'sans-serif' } }, [
			e('h2', { key: 'title', style: { textAlign: 'center' } }, 'Đăng nhập'),
			error ? e('div', { key: 'err', style: { color: 'red', marginBottom: 12 } }, error) : null,
			e('form', { key: 'form', onSubmit }, [
				e('div', { key: 'u', style: { marginBottom: 8 } }, [
					e('label', { htmlFor: 'username', style: { display: 'block', marginBottom: 4 } }, 'Tài khoản'),
					e('input', { id: 'username', value: username, onChange: ev => setUsername(ev.target.value), required: true, style: { width: '100%', padding: 8 } })
				]),
				e('div', { key: 'p', style: { marginBottom: 12 } }, [
					e('label', { htmlFor: 'password', style: { display: 'block', marginBottom: 4 } }, 'Mật khẩu'),
					e('input', { id: 'password', type: 'password', value: password, onChange: ev => setPassword(ev.target.value), required: true, style: { width: '100%', padding: 8 } })
				]),
				e('button', { key: 'submit', type: 'submit', style: { width: '100%', padding: 10 } }, 'Login')
			])
		]);
	}

	const root = ReactDOM.createRoot(document.getElementById('root'));
	root.render(e(App));
})(); 