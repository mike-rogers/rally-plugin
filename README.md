## Jenkins Rally Plug-in
[![Build Status](https://travis-ci.org/mike-rogers/rally-plugin.svg?branch=minor-refactoring)](https://travis-ci.org/mike-rogers/rally-plugin)

This is a Jenkins Plug-in which

1. pulls SCM ChangeSet information from Jenkins Builds and updates relevant Rally defect or story as a build action.
1. picks up Task details [status, actual hrs, todo hrs etc] from scm comments (if provided) and updates rally task details accordingly.
1. saves developers the effort of writing, installing, and maintaining check-in hooks for SCM tools (svn, cvs, perforce etc) in order to update Rally changsets, a task which is especially difficult if your organisation uses more than one configuration management tool.

For more information, please see the [wiki page](https://wiki.jenkins-ci.org/display/JENKINS/Rally+plugin).

### Updating Task Information

The plugin can update your Task information based on the commit messages you supply. Here is a guide:

* `TA12345` -- you can use this syntax to reference a specific Task under a Work Item
* `#3` -- you can also use this syntax. This is read as "Task #3 under the referenced Work Item"
* `status:` udpates the Task's status to one of "In-Progress", "Defined", or "Completed". **Setting a status to "Completed" will automatically set the 'to do' field to 0**
* `todo:` updates the Task's "hours remaining" metric
* `actuals:` updates how long you've spent implementing a specific task
* `estimates:` updates the estimate for a specific task

Each hourly metric is updated with whole numbers only.

Here are some example messages using the above metrics:

```text
US12345: implements task #3 with status: completed; actuals: 5 hours, estimates: 6 hours (updated)
US12345 for task TA54321 with status: in progress; actuals: 3 hours, to do: 15
```

## License

This project is distributed under the MIT license.