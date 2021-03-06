/* global resultsFramework */

//Controller for the header section
resultsFramework.controller('ResultsFrameworkController',
        function($scope,
                $filter,
                orderByFilter,
                ContextMenuSelectedItem,
                DialogService,
                ModalService,
                MetaDataFactory,
                ResultsFrameworkFactory,
                ProgramFactory) {
    
    $scope.model = {    showAddResultsFrameworkDiv: false,
                        showEditResultsFrameworkDiv: false,
                        showStructureResultsFrameworkDiv: false,
                        selectSize: 20,
                        resultsFrameworks: [],
                        fullResultsFramework: {},
                        selectedResultsFramework: {impacts: [], outcomes: [], outputs: [], programms: []},
                        indicatorGroups: [],
                        programms: []
                    };
                    
    $scope.resultsFrameworkForm = {submitted: false};
    
    ResultsFrameworkFactory.getAll().then(function(response){
        $scope.model.resultsFrameworks = response.resultsFrameworks;
    });
    
    ProgramFactory.getAll().then(function(response){
        $scope.model.programms = response.programms;
    });
    
    MetaDataFactory.getAll('indicatorGroups').then(function(idgs){
        $scope.model.impactIndicatorGroups = $filter('filter')(idgs, {indicatorGroupType: "IMPACT"});
        $scope.model.outcomeIndicatorGroups = $filter('filter')(idgs, {indicatorGroupType: "OUTCOME"});
        $scope.model.outputIndicatorGroups = $filter('filter')(idgs, {indicatorGroupType: "OUTPUT"});
    });
            
    $scope.showAddResultsFramework = function(){
        $scope.model.selectedResultsFramework = {impacts: [], outcomes: [], outputs: [], programms: []};
        $scope.model.showAddResultsFrameworkDiv = !$scope.model.showAddResultsFrameworkDiv;
        $scope.model.showStructureResultsFrameworkDiv = false;
        $scope.model.showEditResultsFrameworkDiv = false;
    };
        
    $scope.showEditResultsFramework = function(){        
        $scope.model.selectedResultsFramework = ContextMenuSelectedItem.getSelectedItem();
        $scope.model.showEditResultsFrameworkDiv = true;
        $scope.model.showAddResultsFrameworkDiv = false;
        $scope.model.showStructureResultsFrameworkDiv = false;        
    };
    
    $scope.showStructureResultsFramework = function(){        
        $scope.model.selectedResultsFramework = ContextMenuSelectedItem.getSelectedItem();        
        
        ResultsFrameworkFactory.get($scope.model.selectedResultsFramework.id).then(function(response){
            $scope.model.frameworkStructure = [];
            angular.forEach(orderByFilter(response.impacts, '-name').reverse(), function(im){
                $scope.model.frameworkStructure.push({name: im.name, class: 'impact-row', type: 'IMPACT'});                
                angular.forEach(orderByFilter(im.indicators, '-name').reverse(), function(ind){
                    $scope.model.frameworkStructure.push({name: ind.name, class: 'impact-indicator-row', type: 'INDICATOR'});
                });
            });
            
            angular.forEach(orderByFilter(response.outcomes, '-name').reverse(), function(oc){
                $scope.model.frameworkStructure.push({name: oc.name, class: 'outcome-row', type: 'OUTCOME'});                
                angular.forEach(orderByFilter(oc.indicators, '-name').reverse(), function(ind){
                    $scope.model.frameworkStructure.push({name: ind.name, class: 'outcome-indicator-row', type: 'INDICATOR'});
                });
            });
            
            angular.forEach(orderByFilter(response.programms, '-name').reverse(), function(pr){
                $scope.model.frameworkStructure.push({name: pr.name, class: 'program-row', type: 'PROGRAM'});
                angular.forEach(orderByFilter(pr.outcomes, '-name').reverse(), function(oc){
                    $scope.model.frameworkStructure.push({name: oc.name, class: 'program-outcome-row', type: 'OUTCOME'});                    
                    angular.forEach(orderByFilter(oc.indicators, '-name').reverse(), function(ind){
                        $scope.model.frameworkStructure.push({name: ind.name, class: 'program-outcome-indicator-row', type: 'INDCATOR'});
                    });
                });                
                angular.forEach(orderByFilter(pr.subProgramms, '-name').reverse(), function(sp){
                    $scope.model.frameworkStructure.push({name: sp.name, class: 'subprogram-row', type: 'SUB-PROGRAM'});
                    angular.forEach(orderByFilter(sp.outputs, '-name').reverse(), function(op){
                        $scope.model.frameworkStructure.push({name: op.name, class: 'subprogram-output-row', type: 'OUTPUT'});
                        angular.forEach(orderByFilter(op.indicators, '-name').reverse(), function(ind){
                            $scope.model.frameworkStructure.push({name: ind.name, class: 'subprogram-output-indicator-row', type: 'INDICATOR'});
                        });
                    });
                });
            });
            
            $scope.model.fullResultsFramework = response;
        });
        
        $scope.model.showStructureResultsFrameworkDiv = true;
        $scope.model.showAddResultsFrameworkDiv = false;
        $scope.model.showEditResultsFrameworkDiv = false;
    };
    
    $scope.showActivateResultsFramework = function(){
        
        $scope.model.selectedResultsFramework = ContextMenuSelectedItem.getSelectedItem();
        
        var otherActiveExists = $filter('filter')($scope.model.resultsFrameworks, {active: true}).length > 0;
        if(!$scope.model.selectedResultsFramework.active && otherActiveExists){
            var dialogOptions = {
                headerText: 'error',
                bodyText: 'active_results_framework_exists'
            };
            DialogService.showDialog({}, dialogOptions);            
            return;
        }        
        
        var modalOptions = {
            closeButtonText: 'no',
            actionButtonText: 'yes',
            headerText: $scope.model.selectedResultsFramework.active ? 'deactivate' : 'activate',
            bodyText: $scope.model.selectedResultsFramework.active ? 'are_you_sure_to_activate' : 'are_you_sure_to_deactivate'
        };

        ModalService.showModal({}, modalOptions).then(function(){            
            $scope.model.selectedResultsFramework.active = !$scope.model.selectedResultsFramework.active;            
            ResultsFrameworkFactory.update($scope.model.selectedResultsFramework).then(function(data){
                if (data.response.status === 'ERROR') {
                    var dialogOptions = {
                        headerText: 'framework_saving_error',
                        bodyText: data.message
                    };

                    DialogService.showDialog({}, dialogOptions);
                }
            });
        });        
    };
    
    $scope.hideResultsFrameworkDivs = function(){
        $scope.model.showAddResultsFrameworkDiv = false;
        $scope.model.showEditResultsFrameworkDiv = false;
        $scope.model.showStructureResultsFrameworkDiv = false;
    };
          
    $scope.getSelectedResultsFramework = function(resultsFramework){
        $scope.model.selectedResultsFramework = resultsFramework;
        $scope.model.selectedResultsFramework.impacts = resultsFramework.impacts ?  resultsFramework.impacts : [];
        $scope.model.selectedResultsFramework.outcomes = resultsFramework.outcomes ?  resultsFramework.outcomes : [];
        $scope.model.selectedResultsFramework.outputs = resultsFramework.outputs ?  resultsFramework.outputs : [];
        $scope.model.selectedResultsFramework.programms = resultsFramework.programms ?  resultsFramework.programms : [];
        ContextMenuSelectedItem.setSelectedItem($scope.model.selectedResultsFramework);
    };        
            
    $scope.interacted = function(field, form) {
        var status = false;
        if(!form){
            return status;
        }
        if(field){            
            status = form['submitted'] || field.$dirty;
        }
        return status;        
    };

    $scope.add = function(){
        
        //check for form validity
        $scope.resultsFrameworkForm.submitted = true;        
        if( $scope.resultsFrameworkForm.$invalid ){
            return false;
        }
        
        //form is valid, continue with adding
        ResultsFrameworkFactory.create($scope.model.selectedResultsFramework).then(function(data){
            if (data.response.status === 'ERROR') {
                var dialogOptions = {
                    headerText: 'framework_saving_error',
                    bodyText: data.message
                };

                DialogService.showDialog({}, dialogOptions);
            }
            else {
                
                //add the new results framework to the grid
                var rf = angular.copy($scope.model.selectedResultsFramework);
                rf.id = data.lastImported;                
                $scope.model.resultsFrameworks.splice(0,0,rf);
                    
                //reset form              
                $scope.cancel();
            }
        });
    };
    
    $scope.update = function(){

        //check for form validity
        $scope.resultsFrameworkForm.submitted = true;        
        if( $scope.resultsFrameworkForm.$invalid ){
            return false;
        }

        //form is valid, continue with adding
        ResultsFrameworkFactory.update($scope.model.selectedResultsFramework).then(function(data){
            if (data.response.status === 'ERROR') {
                var dialogOptions = {
                    headerText: 'framework_saving_error',
                    bodyText: data.message
                };

                DialogService.showDialog({}, dialogOptions);
            }
            
            for(var i=0; i<$scope.model.resultsFrameworks.length; i++){
                if( $scope.model.selectedResultsFramework.id === $scope.model.resultsFrameworks[i].id){
                    $scope.model.resultsFrameworks[i] = $scope.model.selectedResultsFramework;
                    break;
                }
            }
            
            //reset form              
            $scope.cancel();
        });
    };
    
    $scope.delete = function(){
        var modalOptions = {
            closeButtonText: 'cancel',
            actionButtonText: 'delete',
            headerText: 'delete',
            bodyText: 'are_you_sure_to_delete'
        };

        ModalService.showModal({}, modalOptions).then(function(){            
            ResultsFrameworkFactory.delete($scope.model.selectedResultsFramework).then(function(){                
                for(var i=0; i<$scope.model.resultsFrameworks.length; i++){
                    if( $scope.model.selectedResultsFramework.id === $scope.model.resultsFrameworks[i].id){
                        $scope.model.resultsFrameworks.splice(i,1);    
                        break;
                    }
                }
                
                $scope.cancel();
                
            }, function(){
                var dialogOptions = {
                    headerText: 'error',
                    bodyText: 'delete_error'
                };
                DialogService.showDialog({}, dialogOptions);
            });
        });
    };
    
    $scope.cancel = function(){
        $scope.resultsFrameworkForm.submitted = false;
        $scope.hideResultsFrameworkDivs();
    };
});