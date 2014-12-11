var attachmentsRequest = 'attachments';

angular.module('RedhatAccess.FUSE', 
  ['ngSanitize',
    'RedhatAccess.header',
    'RedhatAccess.template',
    'RedhatAccess.security',
    'RedhatAccess.search',
    'RedhatAccess.cases',
    'RedhatAccess.logViewer',
    'ui.router',
    'ui.bootstrap' ])
.controller('customCase', ['$scope', '$location', 'securityService', 'NEW_DEFAULTS', function($scope, $location, securityService, NEW_DEFAULTS) {
  NEW_DEFAULTS.product = "Red Hat JBoss Fuse";
  NEW_DEFAULTS.version = "6.2.0";
  
  var params = $location.search();
  
  // handle support case for managed resource
  if ($location.path().indexOf('/resource-case') >= 0) {
    NEW_DEFAULTS.product = params.product;
    NEW_DEFAULTS.version = params.version;
    attachmentsRequest += '?resourceId='+params.resourceId;
    $location.path('/case/new');
  }

   $scope.init = function () {
     securityService.validateLogin(true);
   };
}]);
angular.module('RedhatAccess.cases')
  .controller('BackEndAttachmentsCtrl', ['$scope', 'TreeViewSelectorData', 'AttachmentsService',
    function ($scope, TreeViewSelectorData, AttachmentsService) {
      $scope.name = 'Attachments';
      $scope.attachmentTree = [];
      TreeViewSelectorData.getTree(attachmentsRequest).then(
        function (tree) {
          $scope.attachmentTree = tree;
          AttachmentsService.updateBackEndAttachments(tree);
        },
        function () {
          console.log('Unable to get tree data');
        });
    }
  ]);
  