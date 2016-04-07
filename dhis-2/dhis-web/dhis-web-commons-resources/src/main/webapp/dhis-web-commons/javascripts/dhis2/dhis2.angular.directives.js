/* global moment, angular, directive, dhis2, selection */

'use strict';

/* Directives */

var d2Directives = angular.module('d2Directives', [])


.directive('selectedOrgUnit', function ($timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            
            $("#orgUnitTree").one("ouwtLoaded", function (event, ids, names) {
                if (dhis2.tc && dhis2.tc.metaDataCached) {
                    $timeout(function () {
                        scope.treeLoaded = true;
                        scope.$apply();
                    });
                    selection.responseReceived();
                }
                else {
                    console.log('Finished loading orgunit tree');
                    $("#orgUnitTree").addClass("disable-clicks"); //Disable ou selection until meta-data has downloaded
                    $timeout(function () {
                        scope.treeLoaded = true;
                        scope.$apply();
                    });
                    downloadMetaData();
                }
            });

            //listen to user selection, and inform angular         
            selection.setListenerFunction(setSelectedOu, true);
            function setSelectedOu(ids, names) {
                var ou = {id: ids[0], displayName: names[0]};
                $timeout(function () {
                    scope.selectedOrgUnit = ou;
                    scope.$apply();
                });
            }
        }
    };
})

.directive('d2SetFocus', function ($timeout) {

    return {        
        scope: { trigger: '@d2SetFocus' },
        link: function(scope, element) {
            scope.$watch('trigger', function(value) {
                if(value === "true") { 
                    $timeout(function() {
                        element[0].focus(); 
                    });
                }
            });
        }
    };
})

.directive('d2LeftBar', function () {

    return {
        restrict: 'E',
        templateUrl: 'views/left-bar.html',
        link: function (scope, element, attrs) {

            $("#searchIcon").click(function () {
                $("#searchSpan").toggle();
                $("#searchField").focus();
            });

            $("#searchField").autocomplete({
                source: "../dhis-web-commons/ouwt/getOrganisationUnitsByName.action",
                select: function (event, ui) {
                    $("#searchField").val(ui.item.value);
                    selection.findByName();
                }
            });
        }
    };
})

.directive('blurOrChange', function () {

    return function (scope, elem, attrs) {
        elem.calendarsPicker({
            onSelect: function () {
                scope.$apply(attrs.blurOrChange);
                $(this).change();
            }
        }).change(function () {
            scope.$apply(attrs.blurOrChange);
        });
    };
})

.directive('d2Enter', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if (event.which === 13) {
                scope.$apply(function () {
                    scope.$eval(attrs.d2Enter);
                });
                event.preventDefault();
            }
        });
    };
})

.directive('d2PopOver', function ($compile, $templateCache) {

    return {
        restrict: 'EA',
        scope: {
            content: '=',
            title: '@details',
            template: "@template",
            placement: "@placement",
            trigger: "@trigger"
        },
        link: function (scope, element, attrs) {
            var content = $templateCache.get(scope.template);
            content = $compile(content)(scope);
            scope.content.heading = scope.content.value && scope.content.value.length > 20 ? scope.content.value.substring(0,20).concat('...') : scope.content.value;
            var options = {
                content: content,
                placement: scope.placement ? scope.placement : 'auto',
                trigger: scope.trigger ? scope.trigger : 'hover',
                html: true,
                title: scope.title
            };
            element.popover(options);            
            
            $('body').on('click', function (e) {
                if( !element[0].contains(e.target) ) {
                    element.popover('hide');
                }
            });
        }
    };
})

.directive('d2Sortable', function ($timeout) {

    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            element.sortable({
                connectWith: ".connectedSortable",
                placeholder: "ui-state-highlight",
                tolerance: "pointer",
                handle: '.handle',
                change: function (event, ui) {
                    $timeout(function () {
                        scope.widgetsOrder = getSortedItems(ui);
                        scope.$apply();
                    });

                },
                receive: function (event, ui) {
                    $timeout(function () {
                        scope.widgetsOrder = getSortedItems(ui);
                        scope.$apply();
                    });
                }
            });

            var getSortedItems = function (ui) {
                var biggerWidgets = $("#biggerWidget").sortable("toArray");
                var smallerWidgets = $("#smallerWidget").sortable("toArray");
                var movedIsIdentifeid = false;

                //look for the moved item in the bigger block
                for (var i = 0; i < biggerWidgets.length && !movedIsIdentifeid; i++) {
                    if (biggerWidgets[i] === "") {
                        biggerWidgets[i] = ui.item[0].id;
                        movedIsIdentifeid = true;
                    }
                }

                //look for the moved item in the smaller block
                for (var i = 0; i < smallerWidgets.length && !movedIsIdentifeid; i++) {
                    if (smallerWidgets[i] === "") {
                        smallerWidgets[i] = ui.item[0].id;
                        movedIsIdentifeid = true;
                    }
                }
                return {smallerWidgets: smallerWidgets, biggerWidgets: biggerWidgets};
            };
        }
    };
})

.directive('serversidePaginator', function factory() {

    return {
        restrict: 'E',
        controller: function ($scope, Paginator) {
            $scope.paginator = Paginator;
        },
        templateUrl: '../dhis-web-commons/angular-forms/serverside-pagination.html'
    };
})

.directive('d2CustomDataEntryForm', function ($compile) {
    return{
        restrict: 'E',
        link: function (scope, elm, attrs) {
            scope.$watch('customDataEntryForm', function () {
                if (angular.isObject(scope.customDataEntryForm)) {
                    elm.html(scope.customDataEntryForm.htmlCode);
                    $compile(elm.contents())(scope);
                }
            });
        }
    };
})

.directive('d2CustomRegistrationForm', function ($compile) {
    return{
        restrict: 'E',
        link: function (scope, elm, attrs) {
            scope.$watch('customRegistrationForm', function () {
                if (angular.isObject(scope.customRegistrationForm)) {
                    elm.html(scope.customRegistrationForm.htmlCode);
                    $compile(elm.contents())(scope);
                }
            });
        }
    };
})

/* TODO: this directive access an element #contextMenu somewhere in the document. Looks like it has to be rewritten */
.directive('d2ContextMenu', function () {

    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var contextMenu = $("#contextMenu");

            element.click(function (e) {
                var menuHeight = contextMenu.height();
                var menuWidth = contextMenu.width();
                var winHeight = $(window).height();
                var winWidth = $(window).width();

                var pageX = e.pageX;
                var pageY = e.pageY;

                contextMenu.show();

                if ((menuWidth + pageX) > winWidth) {
                    pageX -= menuWidth;
                }

                if ((menuHeight + pageY) > winHeight) {
                    pageY -= menuHeight;

                    if (pageY < 0) {
                        pageY = e.pageY;
                    }
                }

                contextMenu.css({
                    left: pageX,
                    top: pageY
                });

                return false;
            });

            contextMenu.on("click", "a", function () {
                contextMenu.hide();
            });

            $(document).click(function () {
                contextMenu.hide();
            });
        }
    };
})

.directive('d2Date', function (CalendarService, $parse) {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function (scope, element, attrs, ctrl) {
            var calendarSetting = CalendarService.getSetting();
            var dateFormat = 'yyyy-mm-dd';
            if (calendarSetting.keyDateFormat === 'dd-MM-yyyy') {
                dateFormat = 'dd-mm-yyyy';
            }

            var minDate = $parse(attrs.minDate)(scope),
                    maxDate = $parse(attrs.maxDate)(scope),
                    calendar = $.calendars.instance(calendarSetting.keyCalendar);

            element.calendarsPicker({
                changeMonth: true,
                dateFormat: dateFormat,
                yearRange: '-120:+30',
                minDate: minDate,
                maxDate: maxDate,
                calendar: calendar,
                duration: "fast",
                showAnim: "",
                renderer: $.calendars.picker.themeRollerRenderer,
                onSelect: function () {
                    $(this).change();
                }
            })
                    .change(function () {
                        ctrl.$setViewValue(this.value);
                        this.focus();
                        scope.$apply();
                    });
        }
    };
})

.directive('d2FileInput', function(DHIS2EventService, FileService, DialogService){
    
    return {
        restrict: "A",
        scope: {
            d2FileInputList: '=',
            d2FileInput: '=',
            d2FileInputName: '=',
            d2FileInputCurrentName: '=',
            d2FileInputPs: '='
        },
        link: function (scope, element, attrs) {
            
            var de = attrs.inputFieldId;
            
            var updateEvent = function(singleValue, fullValue){
            	var promise = $http.put('../api/events/' + singleValue.event + '/' + singleValue.dataValues[0].dataElement, singleValue ).then(function(response){
                    return response.data;
               }, function(){
                   dhis2.ec.store.remove('events', fullValue.event);
                   fullValue.id = fullValue.event;
                   dhis2.ec.store.set('events', fullValue);
               });
               return promise;
            }
            
            var updateModel = function () {
                
                var update = scope.d2FileInput.event &&  scope.d2FileInput.event !== 'SINGLE_EVENT' ? true : false;
                
                FileService.upload(element[0].files[0]).then(function(data){
                    
                    if(data && data.status === 'OK' && data.response && data.response.fileResource && data.response.fileResource.id && data.response.fileResource.name){
                                            
                        scope.d2FileInput[de] = data.response.fileResource.id;   
                        scope.d2FileInputCurrentName[de] = data.response.fileResource.name;
                        if( update ){                            
                            if(!scope.d2FileInputName[scope.d2FileInput.event]){
                                scope.d2FileInputName[scope.d2FileInput.event] = [];
                            }                            
                            scope.d2FileInputName[scope.d2FileInput.event][de] = data.response.fileResource.name;
                            
                            var updatedSingleValueEvent = {event: scope.d2FileInput.event, dataValues: [{value: data.response.fileResource.id, dataElement:  de}]};
                            var updatedFullValueEvent = DHIS2EventService.reconstructEvent(scope.d2FileInput, scope.d2FileInputPs.programStageDataElements);
                            updateEvent(updatedSingleValueEvent, updatedFullValueEvent).then(function(data){
                                scope.d2FileInputList = DHIS2EventService.refreshList(scope.d2FileInputList, scope.d2FileInput);
                            });
                        }
                    }
                    else{
                        var dialogOptions = {
                            headerText: 'error',
                            bodyText: 'file_upload_failed'
                        };		
                        DialogService.showDialog({}, dialogOptions);
                    }
                    
                });                 
            };             
            element.bind('change', updateModel);            
        }
    };    
})

.directive('d2FileInputDelete', function($parse, $timeout, FileService, DialogService){
    
    return {
        restrict: "A",
        link: function (scope, element, attrs) {
            var valueGetter = $parse(attrs.d2FileInputDelete);
            var nameGetter = $parse(attrs.d2FileInputName);
            var nameSetter = nameGetter.assign;
            
            if(valueGetter(scope)) {
                FileService.get(valueGetter(scope)).then(function(data){
                    if(data && data.name && data.id){
                        $timeout(function(){
                            nameSetter(scope, data.name);
                            scope.$apply();
                        });
                    }
                    else{
                        var dialogOptions = {
                            headerText: 'error',
                            bodyText: 'file_missing'
                        };		
                        DialogService.showDialog({}, dialogOptions);
                    }                    
                });                 
            }
        }
    };
})

.directive('d2Audit', function () {
    return {
        restrict: 'E',
        template: '<span class="hideInPrint audit-icon" title="{{\'audit_history\' | translate}}" data-ng-click="showAuditHistory()">' +
        '<i class="glyphicon glyphicon-user""></i>' +
        '</span>',
        scope: {
            eventId: '@',
            type: '@',
            nameIdMap: '='
        },
        controller: function ($scope, $modal) {
            $scope.showAuditHistory = function () {
                $modal.open({
                    templateUrl: "../dhis-web-commons/angular-forms/audit-history.html",
                    controller: "AuditHistoryController",
                    resolve: {
                        eventId: function () {
                            return $scope.eventId;
                        },
                        dataType: function () {
                            return $scope.type;
                        },
                        nameIdMap: function () {
                            return $scope.nameIdMap;
                        }
                    }
                })
            }
        },
    };
})
.directive('d2RadioButton', function (){  
    return {
        restrict: 'E',
        templateUrl: '../dhis-web-commons/angular-forms/radio-button.html',
        scope: {
            required: '=dhRequired',
            value: '=dhValue',
            disabled: '=dhDisabled',
            name: '@dhName',            
            customOnClick: '&dhClick',
            currentElement: '=dhCurrentElement',
            event: '=dhEvent',
            id: '=dhId'
        },
        controller: [
            '$scope',
            '$element',
            '$attrs',
            '$q',   
            'CommonUtils',
            function($scope, $element, $attrs, $q, CommonUtils){
                
                $scope.status = "";                
                $scope.clickedButton = "";
                
                $scope.valueClicked = function (buttonValue){
                                        
                    $scope.clickedButton = buttonValue;
                    
                    var originalValue = $scope.value;
                    var tempValue = buttonValue;
                    if($scope.value === buttonValue){
                        tempValue = "";
                    }
                    
                    if(angular.isDefined($scope.customOnClick)){
                        var promise = $scope.customOnClick({value: tempValue});
                        if(angular.isDefined(promise) && angular.isDefined(promise.then)){
                            promise.then(function(status){
                                if(angular.isUndefined(status) || status !== "notSaved"){
                                    $scope.status = "saved";                                    
                                }
                                $scope.value = tempValue;                            
                            }, function(){   
                                $scope.status = "error";
                                $scope.value = originalValue;
                            });
                        }
                        else if(angular.isDefined(promise)){
                            if(promise === false){
                                $scope.value = originalValue;
                            }
                            else {
                                $scope.value = tempValue;
                            }
                        }
                        else{
                            $scope.value = tempValue;
                        }
                    }
                    else{
                        $scope.value = tempValue;
                    }
                };
                
                $scope.getDisabledValue = function(inValue){                    
                    return CommonUtils.displayBooleanAsYesNo(inValue);                    
                };
                
                $scope.getDisabledIcon = function(inValue){                    
                    if(inValue === true || inValue === "true"){
                        return "fa fa-check";
                    }
                    else if(inValue === false || inValue === "false"){
                        return "fa fa-times";
                    }
                    return '';
                }
                
            }],
        link: function (scope, element, attrs) {
            
            scope.radioButtonColor = function(buttonValue){
                
                if(scope.value !== ""){
                    if(scope.status === "saved"){
                        if(angular.isUndefined(scope.currentElement) || (scope.currentElement.id === scope.id && scope.currentElement.event === scope.event)){
                            if(scope.clickedButton === buttonValue){
                                return 'radio-save-success';
                            }
                        }
                    }
                }                
                return 'radio-white';
            };
            
            scope.errorStatus = function(){
                
                if(scope.status === 'error'){
                    if(angular.isUndefined(scope.currentElement) || (scope.currentElement.id === scope.id && scope.currentElement.event === scope.event)){
                        return true;
                    }
                }
                return false;
            };

            scope.radioButtonImage = function(buttonValue){        

                if(angular.isDefined(scope.value)){
                    if(scope.value === buttonValue && buttonValue === "true"){
                        return 'fa fa-stack-1x fa-check';                
                    }            
                    else if(scope.value === buttonValue && buttonValue === "false"){
                        return 'fa fa-stack-1x fa-times';
                    }
                }
                return 'fa fa-stack-1x';        
            };    
        }
    };
})

.directive('dhis2Deselect', function ($document) {
    return {
        restrict: 'A',
        scope: {
            onDeselected: '&dhOnDeselected',
            id: '@dhId',
            preSelected: '=dhPreSelected',
            abortDeselect: '&dhAbortDeselect'
        },
        controller: [
            '$scope',
            '$element',
            '$attrs',
            '$q',            
            function($scope, $element, $attrs, $q){
                
                $scope.documentEventListenerSet = false;
                $scope.elementClicked = false;
                
                $element.on('click', function(event) {                    
                                        
                    $scope.elementClicked = true;
                    if($scope.documentEventListenerSet === false){
                        $document.on('click', $scope.documentClick);
                        $scope.documentEventListenerSet = true;
                    }                             
                });
                
                $scope.documentClick = function(event){
                    var modalPresent = $(".modal-backdrop").length > 0;
                    var calendarPresent = $(".calendars-popup").length > 0;
                    var calendarPresentInEvent = $(event.target).parents(".calendars-popup").length > 0;
                    if($scope.abortDeselect()){
                        $document.off('click', $scope.documentClick);
                        $scope.documentEventListenerSet = false;
                    }else if($scope.elementClicked === false && 
                        modalPresent === false && 
                        calendarPresent === false && 
                        calendarPresentInEvent === false){                        
                        $scope.onDeselected({id:$scope.id});
                        $scope.$apply();  
                        $document.off('click', $scope.documentClick);
                        $scope.documentEventListenerSet = false;
                    }
                    $scope.elementClicked = false;
                };

                if(angular.isDefined($scope.preSelected) && $scope.preSelected === true){
                    $document.on('click', $scope.documentClick);
                    $scope.documentEventListenerSet = true;
                }

            }],
        link: function (scope, element, attrs) {
        }
    };
});
