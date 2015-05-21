## OpenShift Requirements

The following are the configuration steps required to be added to a vanilla OpenShift installation to be able to [Install Fabric8 on OpenShift](fabric8OnOpenShift.html):

### Add roles

The following commands assume you are on the OpenShift master machine in a folder containing the `openshift.local.config` directory:

* Enable the `cluster-admin` role to user `admin`

```
osadm policy add-cluster-role-to-user cluster-admin admin --config=openshift.local.config/master/admin.kubeconfig
```

### Add secrets

* each namespace you wish to install fabric8 into typically requires the `openshift-cert-secrets`


You'll also need to login and switch to the correct project (namespace):

```
osc login
osc project default
```

Then run this command:

```
cat <<EOF | osc create -f -
---
	apiVersion: "v1beta3"
	kind: "Secret"
	metadata:
		name: "openshift-cert-secrets"                                                                                                                                                          
	data:
		root-cert: "$(base64 -w 0 openshift.local.config/master/ca.crt)"
		admin-cert: "$(base64 -w 0 openshift.local.config/master/admin.crt)"
		admin-key: "$(base64 -w 0 openshift.local.config/master/admin.key)"
EOF
```

