package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import jakarta.json.JsonObject;

/** Fictional nomination core and party extension shared by the forms tests. */
final class FormSamples {
    private FormSamples() { }

    static JsonObject json(String text) { return FormJson.parse(text.replace('\'', '"')); }

    static final String CORE = """
        {'format':1,'title':'Nomination application',
         'sections':[
          {'key':'contact','title':'Contact','fields':[
            {'key':'phone','type':'text','label':'Phone','required':true,'maxLength':20},
            {'key':'publicOfficer','type':'boolean','label':'Are you a public officer?','required':true},
            {'key':'resignation','type':'document','label':'Resignation letter','assetType':'DOCUMENT',
             'visibleWhen':{'field':'publicOfficer','equals':true},'requiredWhen':{'field':'publicOfficer','equals':true}}]},
          {'key':'additional','title':'Party questions','fields':[]}],
         'declarations':[{'key':'truth','text':'The information I have given is true.'}],
         'extensionPolicy':{'addFieldsTo':['additional'],'helpEditable':['phone'],'addSections':false,'addDeclarations':true}}
        """;

    static final String EXTENSION = """
        {'format':1,
         'help':{'phone':'Use a number the nomination office can reach.'},
         'addFields':[{'section':'additional','fields':[
            {'key':'disciplinary','type':'boolean','label':'Any previous disciplinary proceedings?','required':true},
            {'key':'details','type':'textarea','label':'Describe them','maxLength':2000,
             'visibleWhen':{'field':'disciplinary','equals':true},'requiredWhen':{'field':'disciplinary','equals':true}},
            {'key':'wards','type':'multichoice','label':'Wards campaigned in','options':[
              {'value':'a','label':'Ward A'},{'value':'b','label':'Ward B'}],'maxItems':2},
            {'key':'officerDetail','type':'text','label':'Ministry','visibleWhen':{'field':'core.publicOfficer','equals':true}}]}],
         'addDeclarations':[{'key':'code','text':'I accept the party code of conduct.'}]}
        """;

    static JsonObject core() { return new FormCompiler().core(json(CORE)); }
    static JsonObject party() { return new FormCompiler().extend(core(), json(EXTENSION)); }
}
