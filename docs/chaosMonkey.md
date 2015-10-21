## Chaos Monkey

Chaos Monkey is a way of testing the resilience of your system by randomly killing [pods](pods.html) to check your system behaves properly. 

Failures will always happen so why not force failures to happen during office hours when your team are watching; rather than at 3AM when you're all asleep?
 
The idea behind Chaos Monkey came from [folks at Netflix](https://github.com/Netflix/SimianArmy/wiki/Chaos-Monkey) who have a bunch of monkeys that make up the [Simian Army](https://github.com/Netflix/SimianArmy/wiki) ;).

### Running Chaos Monkey

Click the `Run...` button on the `apps` tab of the [fabric8 console](console.html) then select the `chaos monkey` app and click the run button.

We highly recommend you run the [Chat](chat.html) application first and login to the chat room so that you can see the monkey awesome take place in the chat room as pods are killed!

You can configure the template of the Chaos Monkey to specifically include or exclude certain kinds of pods using wildcards; chaos monkey can even kill itself which is quite funny ;)


### Demo

Here is a [video demonstrating Chaos Monkey](https://vimeo.com/134822210) using the [ChatOps integration](chat.html)

<div class="row">
  <p class="text-center">
      <iframe src="https://player.vimeo.com/video/134822210" width="1000" height="562" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>
  </p>
</div>

### Installation
    
To install your own Chaos Monkey see the [Install Fabric8 on Kubernetes or OpenShift Guide](getStarted/apps.html)    
