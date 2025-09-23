(function() {
	const root = document.getElementById('profile');

	const firstSegment = window.location.pathname.split('/').filter(Boolean)[0];
	const contextPath = firstSegment ? '/' + firstSegment : '';
	const api = (p) => contextPath + p;
	const goLogin = () => window.location.assign(contextPath + '/');

	function renderProfile(p) {
		root.innerHTML = '';
		const h2 = document.createElement('h2');
		h2.textContent = 'Thông tin người dùng';
		root.appendChild(h2);
		const ul = document.createElement('ul');
		const fields = [
			['Tài khoản', p.username],
			['Họ tên', p.fullName],
			['Email', p.email],
			['SĐT', p.phone],
			['Vai trò', p.role],
			['Trạng thái', p.status],
			['Tạo lúc', p.createdAt],
			['Cập nhật', p.updatedAt]
		];
		fields.forEach(([label, value]) => {
			const li = document.createElement('li');
			li.textContent = label + ': ' + (value ?? '');
			ul.appendChild(li);
		});
		root.appendChild(ul);
	}

	fetch(api('/api/profile'), { credentials: 'include' })
		.then(r => {
			if (r.status === 401) { goLogin(); return null; }
			return r.json();
		})
		.then(p => { if (p) renderProfile(p); })
		.catch(() => goLogin());
})(); 