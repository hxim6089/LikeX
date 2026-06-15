import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import TweetDetailView from '../views/TweetDetailView.vue'
import SearchView from '../views/SearchView.vue'
import NotificationView from '../views/NotificationView.vue'
import MessagesView from '../views/MessagesView.vue'
import AdminView from '../views/AdminView.vue'
import GrokView from '../views/GrokView.vue'
import CompareView from '../views/CompareView.vue'
import AdDashboard from '../views/AdDashboard.vue'
import AnalyticsView from '../views/AnalyticsView.vue'
import TopicView from '../views/TopicView.vue'

const routes = [
    { path: '/', component: HomeView },
    { path: '/grok', component: GrokView },
    { path: '/compare', component: CompareView },
    {
        path: '/analytics',
        component: AnalyticsView,
        beforeEnter: (to, from, next) => {
            isAdminUser() ? next() : next('/');
        }
    },
    { path: '/ad-dashboard', component: AdDashboard },
    { path: '/profile', component: ProfileView },
    { path: '/profile/:id', component: ProfileView },
    { path: '/tweet/:id', component: TweetDetailView },
    { path: '/topic/:name', component: TopicView },
    { path: '/search', component: SearchView },
    { path: '/notifications', component: NotificationView },
    { path: '/messages', component: MessagesView },
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    {
        path: '/admin',
        component: AdminView,
        beforeEnter: (to, from, next) => {
            isAdminUser() ? next() : next('/');
        }
    },
]

const isAdminUser = () => {
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    return user.role?.toUpperCase() === 'ADMIN';
}

const router = createRouter({
    history: createWebHistory(),
    routes,
})

router.beforeEach((to, from, next) => {
    const publicPages = ['/login', '/register'];
    const authRequired = !publicPages.includes(to.path);
    const loggedIn = localStorage.getItem('user');

    if (authRequired && !loggedIn) {
        return next('/login');
    }
    next();
});

export default router
