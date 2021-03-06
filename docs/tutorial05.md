#Introduction

Krail sees Options as the [top layer of configuration](devguide01.md).  Options give users as much control as the Krail developer wants to give them, at runtime.  They can be used for anything which you would typically find in a settings / preferences / options menu. 

#Out of the Box

Let's start with what Krail provides out of the box, the ```SimpleHierarchy```.  When looking for option values, this provides 3 levels:

- the user level value
- the system level value
- a default, hard-coded value

The process is very simple - starting from the top of the hierarchy, the user level, Krail looks for the first defined value, and uses that.  The user and system level would normally be in persistence, and the default coded level is there so that even if persistence is inaccessible, or not yet populated, the system behaves in a predictable way.

This could be described as the user level value overriding the system level, which in turn overrides the default coded level.

#Working example

We will demonstrate this with a page on which the user can select the news topics they wish to see. 

- In the 'pages' package create a new view, 'MyNews', extended from ```Grid3x3View``` 
- Override the ```doBuild()``` method

```java
package com.example.tutorial.pages;

import uk.q3c.krail.core.view.Grid3x3ViewBase;
import uk.q3c.krail.core.view.component.ViewChangeBusMessage;

public class MyNews extends Grid3x3ViewBase {

    @Override
    protected void doBuild(ViewChangeBusMessage busMessage) {
        super.doBuild(busMessage);
    }
}
```
- Add 3 Labels with some example text, for CEO News, Items for Sale and Vacancies
```
 @Override
    protected void doBuild(ViewChangeBusMessage busMessage) {
        super.doBuild(busMessage);
        Label ceoNews = new Label("CEO News");
        Label itemsForSale = new Label("Items for Sale");
        Label vacancies = new Label("Vacancies");
        ceoNews.setSizeFull();
        itemsForSale.setSizeFull();
        vacancies.setSizeFull();
        setMiddleLeft(itemsForSale);
        setCentreCell(ceoNews);
        setMiddleRight(vacancies);
    }
```
- In the 'pages' package create a new direct pages module, "MyOtherPages"
```
package com.example.tutorial.pages;

import com.example.tutorial.i18n.LabelKey;
import uk.q3c.krail.core.navigate.sitemap.DirectSitemapModule;
import uk.q3c.krail.core.shiro.PageAccessControl;


public class MyOtherPages extends DirectSitemapModule {
    /**
     * {@inheritDoc}
     */
    @Override
    protected void define() {
        addEntry("private/my-news", MyNews.class, LabelKey.My_News, PageAccessControl.PERMISSION);
    }
}
```
- Add this new module to the ```BindingManager```
```
    @Override
    protected void addSitemapModules(List<Module> baseModules) {
        baseModules.add(new SystemAccountManagementPages());
        baseModules.add(new MyPages().rootURI("private/finance-department"));
        baseModules.add(new AnnotatedPagesModule());
        baseModules.add(new SystemAdminPages());
        baseModules.add(new MyPublicPages());
        baseModules.add(new MyOtherPages());
    }
```

- add the constant "My_News" to ```LabelKey```
- run the application, log in and navigate to "My News" just to make sure it works.  You should see the three items across the centre of the page.

At the moment these "news channels" will always appear.  Now we need to make them optional - after all, you may not want to see the vacancies, but you will always want to see what the CEO has to say, won't you?

##Setting up the options

In order to use options a class must implement ```OptionContext```

- Modify ```MyNews``` to implement ```OptionContext``` and implement the stubs of the methods.
- create a constructor and inject ```Option``` into it
- annotate the constructor with **@Inject**
- return ```option``` from ```getOption()```

The result should look like this:

```java

public class MyNews extends Grid3x3ViewBase implements OptionContext {

    private Option option;

    @Inject
    public MyNews( Option option) {
        this.option = option;
    }

    @Override
    protected void doBuild(ViewChangeBusMessage busMessage) {
        super.doBuild(busMessage);
        Label ceoNews = new Label("CEO News");
        Label itemsForSale = new Label("Items for Sale");
        Label vacancies = new Label("Vacancies");
        ceoNews.setSizeFull();
        itemsForSale.setSizeFull();
        vacancies.setSizeFull();
        setMiddleLeft(itemsForSale);
        setCentreCell(ceoNews);
        setMiddleRight(vacancies);
    }

  
    @Nonnull
    @Override
    public Option getOption() {
        return option;
    }

  
    @Override
    public void optionValueChanged(Property.ValueChangeEvent event) {

    }
}
```
Options are nothing more than key-value pairs, but we want the keys to be unique across the whole application, and we want them to have a default value so that there is always a value, and, therefore, always predictable behaviour. We will also want them to be presented to users so they can choose a value - which means the option needs a Locale-sensitive name and description. The ```OptionKey``` provides all of these features.

- define a key for each news channel.  

```java
    private final OptionKey<Boolean> ceoVisible = new OptionKey<>(true, this, LabelKey.CEO_News_Channel);
    private final OptionKey<Boolean> itemsForSaleVisible = new OptionKey<>(true, this, LabelKey.Items_For_Sale_Channel);
    private final OptionKey<Boolean> vacanciesVisible = new OptionKey<>(true, this, LabelKey.Vacancies_Channel);
```
The real key - the one that is used in persistence - is made up of the context, the name key and qualifiers (if used).  The context is there to help ensure easily managed uniqueness. Qualifiers are not used in this example, and are only really necessary if you want something like "Push Button 1", "Push Button 2" - you can use the qualifier for the final digit.    

<div class="admonition note">
<p class="first admonition-title">Note</p>
<p class="last">An option value is just an object to Krail. Supported data types will be determined by your choice of persistence</p>
</div>

We will make use of these keys in the ```optionValueChanged``` method, to hide or show the news channels:

- make the ```Label``` items into fields instead of local variables
- add the code to make the channels visible or hidden depending on the option value

```java
    @Override
    public void optionValueChanged(Property.ValueChangeEvent event) {
        ceoNews.setVisible(option.get(ceoVisible));
        itemsForSale.setVisible(option.get(itemsForSaleVisible));
        vacancies.setVisible(option.get(vacanciesVisible));
    }
```
- Finally, we need to make sure these options are processed as part of the build, so we call ```optionValueChanged``` from ```doBuild```
```
 @Override
    protected void doBuild(ViewChangeBusMessage busMessage) {
        super.doBuild(busMessage);
        ceoNews = new Label("CEO News");
        itemsForSale = new Label("Items for Sale");
        vacancies = new Label("Vacancies");
        ceoNews.setSizeFull();
        itemsForSale.setSizeFull();
        vacancies.setSizeFull();
        setMiddleLeft(itemsForSale);
        setCentreCell(ceoNews);
        setMiddleRight(vacancies);
        optionValueChanged(null);
    }
```
Now we have options but we do not have any way of changing them.  We will use ```OptionPopup``` to enable that ...

- Inject ```OptionPopup``` into the constructor
```
  @Inject
    public MyNews(Option option, OptionPopup optionPopup) {
        this.option = option;
        this.optionPopup = optionPopup;
    }
```
- Add a button to ```doBuild()``` to invoke the popup
```
    popupButton=new Button ("options");
    popupButton.addClickListener(event->optionPopup.popup(this,LabelKey.News_Options));
    setBottomCentre(popupButton);
```
This is how the whole class should look now:

```
package com.example.tutorial.pages;

import com.example.tutorial.i18n.LabelKey;
import com.google.inject.Inject;
import com.vaadin.data.Property;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import uk.q3c.krail.core.user.opt.Option;
import uk.q3c.krail.core.user.opt.OptionContext;
import uk.q3c.krail.core.user.opt.OptionKey;
import uk.q3c.krail.core.user.opt.OptionPopup;
import uk.q3c.krail.core.view.Grid3x3ViewBase;
import uk.q3c.krail.core.view.component.ViewChangeBusMessage;

import javax.annotation.Nonnull;

public class MyNews extends Grid3x3ViewBase implements OptionContext {

    private final OptionKey<Boolean> ceoVisible = new OptionKey<>(true, this, LabelKey.CEO_News_Channel);
    private final OptionKey<Boolean> itemsForSaleVisible = new OptionKey<>(true, this, LabelKey.Items_For_Sale_Channel);
    private final OptionKey<Boolean> vacanciesVisible = new OptionKey<>(true, this, LabelKey.Vacancies_Channel);
    private Label ceoNews;
    private Label itemsForSale;
    private Option option;
    private OptionPopup optionPopup;
    private Button popupButton;
    private Button systemOptionButton;
    private Label vacancies;

    @Inject
    public MyNews(Option option, OptionPopup optionPopup) {
        this.option = option;
        this.optionPopup = optionPopup;
    }

    @Override
    protected void doBuild(ViewChangeBusMessage busMessage) {
        super.doBuild(busMessage);
        ceoNews = new Label("CEO News");
        itemsForSale = new Label("Items for Sale");
        vacancies = new Label("Vacancies");
        ceoNews.setSizeFull();
        itemsForSale.setSizeFull();
        vacancies.setSizeFull();

        popupButton = new Button("options");
        popupButton.addClickListener(event -> {
            optionPopup.popup(this, LabelKey.News_Options);
        });

        systemOptionButton = new Button("system option");
        systemOptionButton.addClickListener(event -> {
            option.set(false, 1, ceoVisible);
            optionValueChanged(null);
        });

        setMiddleLeft(itemsForSale);
        setCentreCell(ceoNews);
        setMiddleRight(vacancies);
        setBottomCentre(popupButton);
        setBottomRight(systemOptionButton);

        optionValueChanged(null);
    }

    @Override
    public void optionValueChanged(Property.ValueChangeEvent event) {
        ceoNews.setVisible(option.get(ceoVisible));
        itemsForSale.setVisible(option.get(itemsForSaleVisible));
        vacancies.setVisible(option.get(vacanciesVisible));
    }

    @Nonnull
    @Override
    public Option getOption() {
        return option;
    }
}


```

- Run the application, and login as user "eq"
- Select the "My News" page
- click on the "options" button

The ```OptionPopup``` scans the ```OptionContext``` for ```OptionKey``` fields and presents them for modification by the user

- Un-check the CEO news (he won't know, honestly) , and the CEO channel will disappear (you  might need to move the popup).
- Logout
- Now log in as user "fb"
- Go to the "My News" page and you will find that the CEO channel is back again - because you are a different user
- logout
- log back in as "eq", and as you would expect, the CEO channel is hidden.

We have demonstrated here that options are associated with users.  What we haven't seen is what happens if the system level option changes.  

In fact, at the moment there is no system level, so if there is no user level value, then the default coded value is used.

- Still logged in as user "eq", open the options popup and click "restore to default" for the CEO channel.
- The "CEO News Channel" checkbox becomes checked, and CEO channel re-appears

This is the expected behaviour - we coded a default value of "true" for the ```OptionKey```.  Now to demonstrate changing the system level value:

- In ```doBuild()```, add a new button, "systemOptionButton", and configure it to change the option value at system level 
- We also want to call ```optionValueChanged``` so we can see the impact of the change
- and of course we need to put the button on the page

```
  systemOptionButton = new Button("system option");
  systemOptionButton.addClickListener(event -> {
        option.set(false, 1, ceoVisible);
        optionValueChanged(null);
  });
  setBottomRight(systemOptionButton);
```
- Run the application and login as "eq"
- Navigate to "My News" and you will see that the CEO channel is back - the default ```OptionStore``` is in-memory, so values are lost when we restart the application
- Try pressing "system option".  You will be told that you do not have permission for that action.
- Click on the splash message to clear it

We will come to [User Access Control](tutorial07.md) in detail later, but for now it is enough to know that ```DefaultRealm``` - which provides the authorisation rules - allows users to set their own options, but only allows the 'admin' user to set system level options. 

- Log out, and log back in as 'admin'.  Yes it is the same password.
- Navigate to "My News" and press "system option" again.
- The 'admin' user has permission, so now you will se that the CEO News channel has disappeared.
- press "options" to get the popup, and check "CEO News Channel".
- The item re-appears.
- Press "Reset to Default" for the CEO News Channel and the checkbox is cleared again.
 
This is demonstrating that the "Override" principle mentioned earlier.  If a user has set an option, it is used.  If there is no user level value, the system level value is used.  Failing that, then the hard code default value is used.
 
#Using Hierarchies

If you think about it, this hierarchy principle could be used in other scenarios. You could have hierarchies based on geographic location - maybe *city, country, region*.  Or another based on job - maybe *function, department, team, role*.
The structure of these may be available from other systems - HR, Identity Management, Facilities systems -or you could define them yourself.  You can have as many hierarchies as you wish, and we will come back to this subject later to [create a hierarchy](tutorial15.md) of our own.  
 
 
#Summary

We have:

- introduced options, and their purpose
- demonstrated their hierarchical nature
- seen that user access control is applied to options
- shown that ```OptionKey``` provides a full key definition, enabling the ```OptionPopup``` to populate without any further coding

 
#Download from Github
To get to this point straight from Github, [clone](https://github.com/davidsowerby/krail-tutorial) using branch **step05**
