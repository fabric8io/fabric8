
We love contributions! We really need your help to [fabric8 more awesome](http://fabric8.io/), so please [join our community](/community/index.html)!

Many thanks to all of our [existing contributors](https://github.com/fabric8io/fabric8/graphs/contributors)! Why not dive in and help?

Here's some notes to help you get started:

## Getting Started

* Make sure you have a [GitHub account](https://github.com/signup/free) as you'll need it to submit issues, comments or pull requests.
* Got any ideas for how we can improve fabric8? Please [submit an issue](https://github.com/fabric8io/fabric8/issues?state=open) with your thoughts. Constructive criticism is always greatly appreciated!
* Fancy submitting a blog post, article, or screencast we can link to? Just [submit an issue](https://github.com/fabric8io/fabric8/issues?state=open) and we'll merge it into our website.
* Search [our issue tracker](https://github.com/fabric8io/fabric8/issues?state=open) and see if there's been any ideas or issues reported for what you had in mind; if so please join the conversation in the comments.
* Submit any issues, feature requests or improvement ideas [our issue tracker](https://github.com/fabric8io/fabric8/issues?state=open).
  * Clearly describe the issue including steps to reproduce when it is a bug.
  * Make sure you fill in the earliest version that you know has the issue.

### Fancy hacking some code?

* If you fancy working on some code, check out the these lists of issues:
   * [all open issues](https://github.com/fabric8io/fabric8/issues?state=open) if you fancy being more adventurous.

* To make code changes, fork the repository on GitHub then you can hack on the code. We love any contribution such as:
   * fixing typos
   * improving the documentation or embedded help
   * writing new test cases or improve existing ones
   * adding new features

## Submitting changes to fabric8

Adhering to the following process is the best way to get your work
included in the project:

1. [Fork](https://help.github.com/fork-a-repo/) the project, clone your fork,
   and configure the remotes:

   ```bash
   # Clone your fork of the repo into the current directory
   git clone https://github.com/<your-username>/fabric8.git
   # Navigate to the newly cloned directory
   cd fabric8
   # Assign the original repo to a remote called "upstream"
   git remote add upstream https://github.com/fabric8io/fabric8.git
   ```

2. If you cloned a while ago, get the latest changes from upstream:

   ```bash
   git checkout master
   git pull upstream master
   ```

3. Create a new topic branch (off the main project development branch) to
   contain your feature, change, or fix:

   ```bash
   git checkout -b <topic-branch-name>
   ```

4. Commit your changes in logical chunks. If you have many commits you can use Git's
   [interactive rebase](https://help.github.com/articles/interactive-rebase)
   feature to tidy up your commits before making them public.
   If your change references an existing [issue](https://github.com/fabric8io/fabric8/issues?state=open) then use "fixes #123" in the commit message (using the correct issue number ;).

5. Documentation chagnes is located in the [docs](docs) directory. Take a moment to consider if your code changes 
   should also be documented. You may need to update any of the existing topics, or create a new topic by adding a new `.md` file.
   A new file should be added into the table of contents which is the [summary](docs/SUMMARY.MD) file. 

6. Locally merge (or rebase) the upstream development branch into your topic branch:

   ```bash
   git pull [--rebase] upstream master
   ```

7. Push your topic branch up to your fork:

   ```bash
   git push origin <topic-branch-name>
   ```

8. [Open a Pull Request](https://help.github.com/articles/using-pull-requests/)
    with a clear title and description against the `master` branch.

**IMPORTANT**: By submitting a patch, you agree to allow the project owners to
license your work under the terms of the [Apache License](license.txt)



# Additional Resources

* [fabric8 FAQ](http://fabric8.io/faq/index.html)
* [General GitHub documentation](http://help.github.com/)
* [GitHub create pull request documentation](https://help.github.com/articles/creating-a-pull-request)
* [join the fabric8 community](http://fabric8.io/community/index.html)

