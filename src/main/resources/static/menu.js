(function() {
	const rootMenu = document.getElementById('menu');
	const rootWelcome = document.getElementById('welcome');
	const logoutBtn = document.getElementById('logoutBtn');

	const firstSegment = window.location.pathname.split('/').filter(Boolean)[0];
	const contextPath = firstSegment ? '/' + firstSegment : '';
	const api = (p) => contextPath + p;

	function renderWelcome(fullName) {
		if (!fullName) { 
			rootWelcome.textContent = '';
			logoutBtn.style.display = 'none';
			return; 
		}
		rootWelcome.textContent = 'Welcome, ' + fullName;
		logoutBtn.style.display = '';
	}

	function renderMenu(data) {
		rootMenu.innerHTML = '';
		data.forEach(cat => {
			const h3 = document.createElement('h3');
			h3.textContent = cat.name;
			rootMenu.appendChild(h3);
			const ul = document.createElement('ul');
			cat.products.forEach(p => {
				const li = document.createElement('li');
				li.textContent = p.name + ' - ' + p.price;
				ul.appendChild(li);
			});
			rootMenu.appendChild(ul);
		});
	}

	fetch(api('/api/auth/me'), { credentials: 'include' })
		.then(r => r.ok ? r.json() : null)
		.then(me => renderWelcome(me && me.fullName))
		.catch(() => renderWelcome(''));

	fetch(api('/api/menu'))
		.then(r => r.json())
		.then(renderMenu)
		.catch(() => { rootMenu.textContent = 'Không tải được menu'; });

	logoutBtn.addEventListener('click', function() {
		fetch(api('/api/auth/logout'), { method: 'POST', credentials: 'include' })
			.then(() => window.location.reload())
			.catch(() => window.location.reload());
	});
})(); 