### Configure OpenShift

OpenShift needs some extra installation steps in order to be able to run all the [Fabric8 apps](fabric8Apps.html). Various apps (like [Continuous Delivery](cdelivery.html) and [MQ](mq.html) requires secrets and service accounts to be setup). 

### Add roles

The following commands assume you are on the OpenShift master machine :

* Enable the `cluster-admin` role to user `admin`

```
oadm policy add-cluster-role-to-user cluster-admin admin
```

### Enable the OpenShift router

The [OpenShift Router](https://docs.openshift.org/latest/architecture/core_concepts/routes.html#haproxy-template-router) enables external access to services inside a Kubernetes cluster using haproxy; e.g. so you can access web apps from your browser for apps running inside Kubernetes.

Either try following the [Router installation documentation](https://docs.openshift.org/latest/admin_guide/install/deploy_router.html) or try this command:

```
oadm router --create --credentials=/var/lib/openshift/openshift.local.config/master/openshift-router.kubeconfig
```

### Add secrets and service accounts

Run the following on the master node; assuming `/var/lib/openshift/openshift.local.config/` is where the local configuration is installed for OpenShift:

```
oc delete scc restricted
cat <<EOF | oc create -f -
---
  apiVersion: v1
  groups:
  - system:authenticated
  kind: SecurityContextConstraints
  metadata:
    name: restricted
  runAsUser:
    type: RunAsAny
  seLinuxContext:
    type: MustRunAs
EOF
oc delete scc privileged
cat <<EOF | oc create -f -
---
  allowHostDirVolumePlugin: true
  allowPrivilegedContainer: true
  apiVersion: v1
  groups:
  - system:cluster-admins
  - system:nodes
  kind: SecurityContextConstraints
  metadata:
    name: privileged
  runAsUser:
    type: RunAsAny
  seLinuxContext:
    type: RunAsAny
  users:
  - system:serviceaccount:openshift-infra:build-controller
  - system:serviceaccount:default:default
  - system:serviceaccount:default:fabric8
EOF
cat <<EOF | oc create -f -
---
  apiVersion: "v1"
  kind: "Secret"
  metadata:
    name: "openshift-cert-secrets"
  data:
    root-cert: "$(base64 -w 0 /var/lib/openshift/openshift.local.config/master/ca.crt)"
    admin-cert: "$(base64 -w 0 /var/lib/openshift/openshift.local.config/master/admin.crt)"
    admin-key: "$(base64 -w 0 /var/lib/openshift/openshift.local.config/master/admin.key)"
EOF
cat <<EOF | oc create -f -
---
  apiVersion: v1
  kind: ServiceAccount
  metadata:
    name: fabric8
  secrets:
    -
      name: openshift-cert-secrets
EOF
cat <<EOF | oc create -f -
---
  apiVersion: v1
  kind: ServiceAccount
  metadata:
    name: metrics
EOF
oadm policy add-cluster-role-to-user cluster-reader system:serviceaccount:default:metrics
```
