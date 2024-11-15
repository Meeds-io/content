import * as newsTargetingService from '../../services/newsTargetingService.js';
import * as newsService from '../../services/newsServices.js';


if (!Vue.prototype.$newsTargetingService) {
  window.Object.defineProperty(Vue.prototype, '$newsTargetingService', {
    value: newsTargetingService,
  });
}
if (!Vue.prototype.$newsService) {
  window.Object.defineProperty(Vue.prototype, '$newsService', {
    value: newsService,
  });
}
