``getTimeTreesIterate`` is designed to run each analysis iteratively,
checking for convergence after each iteration. If the analyses converge
before reaching the max iterations, it will stop early. It will run each
analysis for ``mcmc_length`` MCMC samples (here, ``1e5``), and it will repeat
this up to ``iterations`` times (here, ``4``), so here we have a maximum of
4e5 MCMC samples, which is far too few to expect convergence.

The convergence check is based on the ESS of the parameters reported in the
log files. You can exclude parameters from this ESS check using the ``ignore``
argument (here, we ignore ``freqParameter``, as it is a fixed value).

The effective sample size (ESS) of a parameter is a measure of how much
independent information your MCMC sample contains. Even though MCMC generates many
samples, they are typically autocorrelated -- each sample depends on the previous
one. ESS is the number of independent draws from the target distribution with the
same estimation power and can be thought of as the sample size for that parameter.

A higher ESS means your sample more reliably represents the posterior
distribution. Low ESS indicates that you need more information, which
can indicate the need for longer MCMC runs. We typically recommend an
ESS of at least 200 for each estimated parameter.


