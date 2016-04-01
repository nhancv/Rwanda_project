/* global resultsFramework */

//Controller for the header section
resultsFramework.controller('ProjectController', 
        function($scope,
                $filter,
                DialogService, 
                ModalService, 
                ProjectFactory, 
                MetaDataFactory,
                DataSetFactory,
                MetaAttributesFactory) {
    
    $scope.maxOptionSize = 30;
    $scope.model = {    showAddProjectDiv: false,
                        showEditProject: false,
                        selectSize: 20,
                        projects: [],
                        donorList: [],
                        selectedProject: {},
                        budgetDataSets: [],
                        indicatorGroups: []
                    };

    $scope.projectForm = {submitted: false};
    
    MetaDataFactory.getAll('indicatorGroups').then(function(idgs){
        $scope.model.impactIndicatorGroups = $filter('filter')(idgs, {indicatorGroupType: "IMPACT"});;
        $scope.model.outcomeIndicatorGroups = $filter('filter')(idgs, {indicatorGroupType: "OUTCOME"});;
        $scope.model.outputIndicatorGroups = $filter('filter')(idgs, {indicatorGroupType: "OUTPUT"});;
    });
    
    ProjectFactory.getAll().then(function(response){
        $scope.model.projects = response.projects ? response.projects : [];
    });
    
    DataSetFactory.getBudgetDataSets().then(function(ds){
        $scope.model.budgetDataSets = ds ? ds : [];
    });
    
    MetaAttributesFactory.getProjectAttributes().then(function(response){
        if(response && response.attributes){            
            angular.forEach(response.attributes, function(att){
                if(att.code === 'donorList' && att.optionSet && att.optionSet.options && att.optionSet.options.length > 0){
                    $scope.model.donorList = att.optionSet.options;
                }
            });
        }
    });
    
    $scope.showAddProject = function(){
        $scope.model.showAddProjectDiv = !$scope.model.showAddProjectDiv;
    };
    
    $scope.showEditProject = function(project){
        $scope.model.selectedProject = project;
        $scope.model.showAddProjectDiv = false;
        $scope.model.showEditProject = true;
    };
    
    $scope.hideAddProject = function(){
        $scope.model.showAddProjectDiv = false;
        $scope.model.showEditProject = false;
        $scope.model.selectedProject = {};
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
    
    $scope.addProject = function(){
        
        //check for form validity
        $scope.projectForm.submitted = true;        
        if( $scope.projectForm.$invalid ){            
            return false;
        }
        
        //form is valid, continue with adding
        ProjectFactory.create($scope.model.selectedProject).then(function(data){
            if (data.response.status === 'ERROR') {
                var dialogOptions = {
                    headerText: 'project_saving_error',
                    bodyText: data.message
                };

                DialogService.showDialog({}, dialogOptions);
            }
            else {
                
                //add the new project to the grid
                var pr = angular.copy($scope.model.selectedProject);
                pr.id = data.lastImported;                
                $scope.model.projects.splice(0,0,pr);
                    
                //reset form              
                $scope.cancel();
            }
        });
    };
    
    $scope.updateProject = function(){

        //check for form validity
        $scope.projectForm.submitted = true;        
        if( $scope.projectForm.$invalid ){
            return false;
        }
        
        //form is valid, continue with adding
        ProjectFactory.update($scope.model.selectedProject).then(function(data){
            if (data.response.status === 'ERROR') {
                var dialogOptions = {
                    headerText: 'project_saving_error',
                    bodyText: data.message
                };

                DialogService.showDialog({}, dialogOptions);
            }
            
            for(var i=0; i<$scope.model.projects.length; i++){
                if( $scope.model.selectedProject.id === $scope.model.projects[i].id){
                    $scope.model.projects[i] = $scope.model.selectedProject;
                    break;
                }
            }
            
            //reset form              
            $scope.cancel();
        });
    };
    
    $scope.deleteProject = function(){
        var modalOptions = {
            closeButtonText: 'cancel',
            actionButtonText: 'delete',
            headerText: 'delete',
            bodyText: 'are_you_sure_to_delete'
        };

        ModalService.showModal({}, modalOptions).then(function(){            
            ProjectFactory.delete($scope.model.selectedProject).then(function(){                
                for(var i=0; i<$scope.model.projects.length; i++){
                    if( $scope.model.selectedProject.id === $scope.model.projects[i].id){
                        $scope.model.projects.splice(i,1);    
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
        $scope.projectForm.submitted = false;
        $scope.hideAddProject();
    };
});