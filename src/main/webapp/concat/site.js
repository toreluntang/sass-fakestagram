if (typeof File === 'undefined') {
    File = function() {
        return false;
    };
}
if (typeof console === 'undefined') {
  console = {
    log: function() {}
  };
}
(function() {

config.$inject = ['$stateProvider', '$urlRouterProvider'];
AuthCtrl.$inject = ['CrudService'];
ProfileCtrl.$inject = ['$rootScope', '$scope', 'DataService', 'CrudService'];
CrudService.$inject = ['$q', '$http'];
DataService.$inject = ['$q', '$http'];angular.element(document).ready(function (event) {
    console.log("angular is ready test");

    var modules = [];
    modules.push('ui.router'); 

    angular
        .module('mySASSFrontend', modules)
        .value('events', {

        })

        .controller('AuthCtrl', AuthCtrl)
        .controller('ProfileCtrl', ProfileCtrl)
        .factory('CrudService', CrudService)
        .factory('DataService', DataService)
   

        .config(config)        
        .run(run);
        
     
    angular.bootstrap(angular.element('#ng-app'), ['mySASSFrontend']);
});
/**
 * @ngInject
 */
function config ($stateProvider, $urlRouterProvider) {

    var url = '';
	var apiPaths = {
    	login: url + '/login',
    	createAcc: url + '/account/create'
	};

    $urlRouterProvider.otherwise('/welcome');
    
    $stateProvider
        .state('welcome', {
            url: '/welcome',
            templateUrl: 'views/login.html',
            controller: 'AuthCtrl as auth'
        })
        .state('profile', {
            url: '/profile',
            templateUrl: 'views/profile.html',
            controller: 'ProfileCtrl as profile'
        });
}
/**
 * @ngInject
 */
function run () {

}
/**
 * @ngInject
 */
function AuthCtrl(CrudService) {
    vm = this;
    vm.test = 'altceva';

    vm.login = function login() {
    var username = 'test';
    var password = 'pass';
    var loginData = {
    	username: username,
    	password: password
    	};
    var promise = CrudService.createItem(loginData,apiPaths['login']);
    }
}
/**
 * @ngInject
 */
function ProfileCtrl($rootScope, $scope, DataService, CrudService) {
    vm = this;

    vm.profiletest = "Profile Test";
    vm.myPic = "";
    vm.userId = '1';
    vm.imageId = '1';
    // vm.showComments = false;


    
    function onLoadImagesSuccess(imagesData) {
    	console.log("onLoadImagsSuccess RIGHT ONE")
        _(imagesData).forEach(function(n) { 
            console.log(n)
            n.showComments = false;
            DataService.loadStuff('http://localhost:8080/sec/resources/comment?imageId=' + n.imageid)
            .then(angular.bind(this, onLoadImageCommentsSuccess), angular.bind(this, onLoadImageCommentsError));       

            function onLoadImageCommentsSuccess(data) {
                console.log("onLoadImageCommentsSuccess", data)
                n.imageComments = data
            }
            function onLoadImageCommentsError(error) {
                console.log("onLoadImageCommentsError: no comments found for this image", error)

            }
        });
        console.log("Images after adding comments are: ", imagesData)
        vm.pictures = imagesData;


        
        

    }
    function onLoadImagesError(error) {
    	console.log("onLoadImagsError", error)
    }
    function onUploadPicSuccess(data) {
        console.log("onUploadPicSuccess", data)
    }
    function onLoadImageCommentsError(error) {
        console.log("onLoadImageCommentsError", error)
    }
    function onCreateCommentSuccess(data) {
        console.log("onCreateCommentSuccess", data)
        getAllImages(vm.userId);
    }
    function onCreateCommentError(error) {
        console.log("onCreateCommentError", error)
    }


    function onLoadUsersSuccess(usersData) {
        console.log("onLoadUsersSuccess", usersData)
    }
    function onLoadUsersError(error) {
        console.log("onLoadUsersError", error)
    }

    vm.uploadPic = function uploadPic(myPic) {
        console.log("uploadPic clicked!!!", vm.myPic, myPic)
        // var fd = new FormData();
        // fd.append('file', vm.myPic);
        // CrudService.createItem(fd, '/resources/file?userid=1')
        //     .then(angular.bind(this, onUploadPicSuccess), angular.bind(this, onUploadPicError));
    }

    vm.addComment = function addComment(commBody, imageId) {
        commentData = {comment: commBody, userId: vm.userId, imageId: imageId};
        vm.commBody = "";
        // console.log("commentData to be added is: ", commentData);
        CrudService.createItem(commentData, 'http://localhost:8080/sec/resources/comment')
            .then(angular.bind(this, onCreateCommentSuccess), angular.bind(this, onCreateCommentError));
        
    }

    // vm.toggleComments = function toggleComments() {
    //     vm.showComments = !vm.showComments;
    // }


    

    function getAllImages(userId) {
        DataService.loadStuff('http://localhost:8080/sec/resources/file/getallimages?id='+userId)
            .then(angular.bind(this, onLoadImagesSuccess), angular.bind(this, onLoadImagesError));
    }
    function getAllUsers() {
        DataService.loadStuff('http://localhost:8080/sec/resources/file/accounts')
            .then(angular.bind(this, onLoadUsersSuccess), angular.bind(this, onLoadUsersError));
    }

   getAllImages(vm.userId);

}
/**
 * @ngInject
 */

function CrudService($q, $http) {
    
    var service = {
        createItem: createItem,
        updateItem: updateItem,
        deleteItem: deleteItem
    };
    return service;

    function createAuthorizationHeader(uploadUrl, method) {
        var credentials = {
            id: 'd2b97532-e8c5-e411-8270-f0def103cfd0',
            algorithm: 'sha256',
            key: '7b76ae41-def3-e411-8030-0c8bfd2336cd'
        };
        var options = {
            credentials: credentials,
            ext: 'XRequestHeaderToProtect:secret'
        };
        var autourl = window.location.href
        var arr = autourl.split('/');
        autourl = arr[0] + '//' + arr[2];
        var header = hawk.client.header(autourl + uploadUrl, method, options);
        if (header.err != null) {
            alert(header.err);
            return null;
        }
        else
            return header;
    }

    // implementation
    function createItem(objData, url) {
        var def = $q.defer();
        console.log(objData)
        $http({
            method: 'POST',
            url: url,
            headers: { 'Content-Type' : 'application/x-www-form-urlencoded' },
            data: $.param(objData)
        })
        .success(function(data) {
            def.resolve(data);
        })
        .error(function() {
            def.reject("Failed to create item");
        });
        return def.promise;
    }
    //  function createItem(objData, url) { // PAUL
    //     var def = $q.defer();
    //     console.log(objData)
    //     var header = createAuthorizationHeader(url,'POST');
    //     $http.post(url, objData, {
    //         transformRequest: angular.identity,
    //         headers: {
    //             'Content-Type': undefined,
    //             'XRequestHeaderToProtect': 'secret',
    //             'Authorization': header.field
    //         }
    //     })
    //     .success(function(data) {
    //         def.resolve(data);
    //     })
    //     .error(function() {
    //         def.reject("Failed to create item");
    //     });
    //     return def.promise;
    // }
    function updateItem(objData, url) {
        var def = $q.defer();
        var header = createAuthorizationHeader(url,'POST');
        $http.post(url, objData, {
            transformRequest: angular.identity,
            headers: {
                'Content-Type': undefined,
                'XRequestHeaderToProtect': 'secret',
                'Authorization': header.field
            }
        })
        .success(function(data) {
            def.resolve(data);
        })
        .error(function() {
            def.reject("Failed to update item");
        });
        return def.promise;
    }
    function deleteItem(objData, url) {
        var def = $q.defer();
        $http.delete(url + '/' + objData.id)
            .success(function(data) {
                def.resolve(data);
            })
            .error(function() {
                def.reject("Failed to delete item with id" + objData.id);
            });
            return def.promise;
    };

}
/**
 * @ngInject
 */

function DataService($q, $http) {
    
    var service = {
        loadStuff: loadStuff,
       
    };
    return service;

    // implementation
    function loadStuff(url) {
        var def = $q.defer();

        $http.get(url)
            .success(function(data) {
                def.resolve(data);
            })
            .error(function() {
                def.reject("Failed to load " + url);
            });
        return def.promise;
    }
}
})();
//# sourceMappingURL=site.js.map